import { serve } from "https://deno.land/std@0.208.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import Anthropic from "https://esm.sh/@anthropic-ai/sdk@0.24.0";

const anthropic = new Anthropic({
  apiKey: Deno.env.get("ANTHROPIC_API_KEY") ?? "",
});

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers":
    "authorization, x-client-info, apikey, content-type",
};

interface RelayRequest {
  action: "generate_insight";
  date: string; // ISO-8601 date string, e.g. "2025-01-15"
}

serve(async (req: Request) => {
  // Handle CORS preflight
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    // Authenticate the request via Supabase JWT
    const supabase = createClient(
      Deno.env.get("SUPABASE_URL") ?? "",
      Deno.env.get("SUPABASE_ANON_KEY") ?? "",
      {
        global: { headers: { Authorization: req.headers.get("Authorization") ?? "" } },
      }
    );

    const { data: { user }, error: authError } = await supabase.auth.getUser();
    if (authError || !user) {
      return new Response(JSON.stringify({ error: "Unauthorized" }), {
        status: 401,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const body: RelayRequest = await req.json();

    if (body.action === "generate_insight") {
      // Fetch the user's usage data for the requested date from the DB
      const { data: usageData } = await supabase
        .from("app_usage_entries")
        .select("*")
        .eq("user_id", user.id)
        .gte("start_time", `${body.date}T00:00:00Z`)
        .lt("start_time", `${body.date}T23:59:59Z`)
        .order("duration_ms", { ascending: false })
        .limit(20);

      const totalMinutes = (usageData ?? []).reduce(
        (sum: number, entry: { duration_ms: number }) =>
          sum + Math.round(entry.duration_ms / 60000),
        0
      );

      const topApps = (usageData ?? [])
        .slice(0, 5)
        .map((e: { app_name: string; duration_ms: number }) => ({
          name: e.app_name,
          minutes: Math.round(e.duration_ms / 60000),
        }));

      const prompt = `
You are a compassionate digital wellness coach. Given the following screen time data for ${body.date}, provide:
1. A brief, encouraging summary (2-3 sentences)
2. Three specific, actionable suggestions
3. Two positive highlights from the data

Screen time data:
- Total screen time: ${totalMinutes} minutes
- Top apps: ${JSON.stringify(topApps)}

Respond with a JSON object with keys: summary (string), suggestions (string[3]), highlights (string[2]).
`.trim();

      const message = await anthropic.messages.create({
        model: "claude-3-5-haiku-20241022",
        max_tokens: 512,
        messages: [{ role: "user", content: prompt }],
      });

      const content = message.content[0];
      if (content.type !== "text") {
        throw new Error("Unexpected response type from Anthropic");
      }

      // Extract JSON from the response
      const jsonMatch = content.text.match(/\{[\s\S]*\}/);
      if (!jsonMatch) throw new Error("No JSON found in Anthropic response");
      const insightData = JSON.parse(jsonMatch[0]);

      // Persist the insight
      const { data: insight } = await supabase
        .from("daily_insights")
        .upsert({
          user_id: user.id,
          date: body.date,
          summary: insightData.summary,
          highlights: insightData.highlights,
          suggestions: insightData.suggestions,
          total_screen_time_minutes: totalMinutes,
          top_apps: topApps,
          tier: 3,
        })
        .select()
        .single();

      return new Response(JSON.stringify(insight), {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    return new Response(JSON.stringify({ error: "Unknown action" }), {
      status: 400,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  } catch (error) {
    console.error("ai-relay error:", error);
    return new Response(
      JSON.stringify({ error: error instanceof Error ? error.message : "Internal error" }),
      {
        status: 500,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      }
    );
  }
});
