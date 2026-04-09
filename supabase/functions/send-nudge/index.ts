/**
 * send-nudge — Spark Edge Function
 *
 * Creates a nudge_event record and triggers a push notification to the
 * recipient via FCM (Android) or APNs (iOS) using the platform-appropriate
 * token stored in the `device_tokens` table.
 *
 * POST /functions/v1/send-nudge
 * Authorization: Bearer <sender-jwt>
 * Body:
 *   {
 *     "recipient_user_id": "<uuid>",
 *     "message": "You've got this! Take a break. 🌿",   // optional custom message
 *     "nudge_type": "encouragement" | "check_in" | "challenge"
 *   }
 *
 * Response 201:
 *   { "nudge_event_id": "<uuid>", "delivered": true }
 */

import { createClient } from "npm:@supabase/supabase-js@2";

const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers":
    "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

const VALID_NUDGE_TYPES = ["encouragement", "check_in", "challenge"] as const;
type NudgeType = typeof VALID_NUDGE_TYPES[number];

const DEFAULT_MESSAGES: Record<NudgeType, string> = {
  encouragement: "Your buddy is cheering you on! Keep going. ⚡",
  check_in: "Your buddy wants to know how you're doing. Take a moment to reflect. 🧠",
  challenge: "Your buddy has issued a challenge! Are you up for it? 🏆",
};

/** Send an FCM push notification. Returns true on success. */
async function sendFcmNotification(
  fcmToken: string,
  title: string,
  body: string,
  fcmServerKey: string,
): Promise<boolean> {
  const response = await fetch("https://fcm.googleapis.com/fcm/send", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "Authorization": `key=${fcmServerKey}`,
    },
    body: JSON.stringify({
      to: fcmToken,
      notification: { title, body },
      data: { type: "nudge" },
      priority: "high",
    }),
  });

  if (!response.ok) {
    console.error("send-nudge: FCM error", response.status, await response.text());
    return false;
  }

  const result = await response.json();
  return result.success === 1;
}

/** Send an APNs push notification via the Firebase HTTP v1 API (unified endpoint). */
async function sendApnsNotification(
  apnsToken: string,
  title: string,
  body: string,
  fcmServerKey: string,
  fcmProjectId: string,
): Promise<boolean> {
  const response = await fetch(
    `https://fcm.googleapis.com/v1/projects/${fcmProjectId}/messages:send`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${fcmServerKey}`,
      },
      body: JSON.stringify({
        message: {
          token: apnsToken,
          notification: { title, body },
          apns: {
            payload: {
              aps: { alert: { title, body }, sound: "default", badge: 1 },
            },
          },
        },
      }),
    },
  );

  if (!response.ok) {
    console.error("send-nudge: APNs/FCMv1 error", response.status, await response.text());
    return false;
  }

  return true;
}

Deno.serve(async (req: Request): Promise<Response> => {
  if (req.method === "OPTIONS") {
    return new Response(null, { status: 204, headers: CORS_HEADERS });
  }

  if (req.method !== "POST") {
    return new Response(
      JSON.stringify({ error: "Method not allowed" }),
      { status: 405, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  const authHeader = req.headers.get("Authorization");
  if (!authHeader?.startsWith("Bearer ")) {
    return new Response(
      JSON.stringify({ error: "Missing or invalid Authorization header" }),
      { status: 401, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  let body: { recipient_user_id?: string; message?: string; nudge_type?: string };
  try {
    body = await req.json();
  } catch {
    return new Response(
      JSON.stringify({ error: "Invalid JSON body" }),
      { status: 400, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  const { recipient_user_id, nudge_type, message } = body;

  if (!recipient_user_id) {
    return new Response(
      JSON.stringify({ error: "Field 'recipient_user_id' is required" }),
      { status: 400, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  if (!nudge_type || !VALID_NUDGE_TYPES.includes(nudge_type as NudgeType)) {
    return new Response(
      JSON.stringify({ error: `Field 'nudge_type' must be one of: ${VALID_NUDGE_TYPES.join(", ")}` }),
      { status: 400, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  const resolvedType = nudge_type as NudgeType;
  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const supabaseAnonKey = Deno.env.get("SUPABASE_ANON_KEY");
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  const fcmServerKey = Deno.env.get("FCM_SERVER_KEY");
  const fcmProjectId = Deno.env.get("FCM_PROJECT_ID");

  if (!supabaseUrl || !supabaseAnonKey || !serviceRoleKey || !fcmServerKey || !fcmProjectId) {
    return new Response(
      JSON.stringify({ error: "Server configuration error" }),
      { status: 500, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  const supabaseUser = createClient(supabaseUrl, supabaseAnonKey, {
    global: { headers: { Authorization: authHeader } },
  });
  const { data: { user }, error: authError } = await supabaseUser.auth.getUser();
  if (authError || !user) {
    return new Response(
      JSON.stringify({ error: "Unauthorized" }),
      { status: 401, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  if (user.id === recipient_user_id) {
    return new Response(
      JSON.stringify({ error: "You cannot nudge yourself" }),
      { status: 400, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  const supabaseAdmin = createClient(supabaseUrl, serviceRoleKey);

  // Verify a buddy relationship exists
  const { data: buddyPair } = await supabaseAdmin
    .from("buddy_pairs")
    .select("id")
    .or(
      `and(user_a.eq.${user.id},user_b.eq.${recipient_user_id}),and(user_a.eq.${recipient_user_id},user_b.eq.${user.id})`,
    )
    .maybeSingle();

  if (!buddyPair) {
    return new Response(
      JSON.stringify({ error: "You can only nudge your buddies" }),
      { status: 403, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  // Resolve the notification message
  const notificationBody = message?.trim() || DEFAULT_MESSAGES[resolvedType];
  const notificationTitle = "Spark — Buddy Nudge ⚡";

  // Create nudge_event record
  const { data: nudgeEvent, error: nudgeError } = await supabaseAdmin
    .from("nudge_events")
    .insert({
      sender_id: user.id,
      recipient_id: recipient_user_id,
      nudge_type: resolvedType,
      message: notificationBody,
      buddy_pair_id: buddyPair.id,
    })
    .select("id")
    .single();

  if (nudgeError || !nudgeEvent) {
    console.error("send-nudge: nudge_events insert error", nudgeError);
    return new Response(
      JSON.stringify({ error: "Failed to create nudge event" }),
      { status: 500, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  // Fetch the recipient's device token(s)
  const { data: tokens } = await supabaseAdmin
    .from("device_tokens")
    .select("token, platform")
    .eq("user_id", recipient_user_id)
    .eq("active", true);

  let delivered = false;

  if (tokens && tokens.length > 0) {
    for (const { token, platform } of tokens) {
      try {
        if (platform === "android") {
          delivered = await sendFcmNotification(token, notificationTitle, notificationBody, fcmServerKey) || delivered;
        } else if (platform === "ios") {
          delivered = await sendApnsNotification(token, notificationTitle, notificationBody, fcmServerKey, fcmProjectId) || delivered;
        }
      } catch (err) {
        console.error(`send-nudge: push delivery failed for platform=${platform}`, err);
      }
    }
  }

  return new Response(
    JSON.stringify({ nudge_event_id: nudgeEvent.id, delivered }),
    {
      status: 201,
      headers: { ...CORS_HEADERS, "Content-Type": "application/json" },
    },
  );
});
