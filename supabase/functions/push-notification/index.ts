import { serve } from "https://deno.land/std@0.208.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers":
    "authorization, x-client-info, apikey, content-type",
};

interface PushPayload {
  userId: string;
  title: string;
  body: string;
  data?: Record<string, string>;
}

serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    // This endpoint is called server-side (e.g. from a database trigger)
    // so we authenticate with the service role key.
    const supabase = createClient(
      Deno.env.get("SUPABASE_URL") ?? "",
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? ""
    );

    const payload: PushPayload = await req.json();

    // Look up the user's push token(s)
    const { data: tokens } = await supabase
      .from("push_tokens")
      .select("token, platform")
      .eq("user_id", payload.userId);

    if (!tokens || tokens.length === 0) {
      return new Response(
        JSON.stringify({ message: "No push tokens found for user" }),
        { headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const results = await Promise.allSettled(
      tokens.map(async (tokenEntry: { token: string; platform: string }) => {
        if (tokenEntry.platform === "android") {
          return sendFcmNotification(tokenEntry.token, payload);
        } else if (tokenEntry.platform === "ios") {
          return sendApnsNotification(tokenEntry.token, payload);
        }
      })
    );

    return new Response(JSON.stringify({ results }), {
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  } catch (error) {
    console.error("push-notification error:", error);
    return new Response(
      JSON.stringify({ error: error instanceof Error ? error.message : "Internal error" }),
      {
        status: 500,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      }
    );
  }
});

async function sendFcmNotification(token: string, payload: PushPayload) {
  const fcmServerKey = Deno.env.get("FCM_SERVER_KEY") ?? "";
  const response = await fetch("https://fcm.googleapis.com/fcm/send", {
    method: "POST",
    headers: {
      Authorization: `key=${fcmServerKey}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      to: token,
      notification: {
        title: payload.title,
        body: payload.body,
      },
      data: payload.data ?? {},
      priority: "normal",
    }),
  });
  return response.json();
}

async function sendApnsNotification(token: string, payload: PushPayload) {
  // In production, use APNs HTTP/2 with JWT authentication.
  // This is a simplified placeholder.
  console.log(`APNs notification to ${token}: ${payload.title}`);
  return { status: "queued" };
}
