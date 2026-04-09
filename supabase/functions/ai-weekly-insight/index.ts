/**
 * ai-weekly-insight — Bilbo Edge Function
 *
 * Receives an anonymized weekly summary from the mobile app, calls the
 * Anthropic API (claude-sonnet-4-20250514) to generate a personalized
 * 200-word narrative insight, and returns the result.
 *
 * Rate limited to 1 call per user per ISO week. Rate limit state is
 * stored in the `ai_insight_rate_limits` table.
 *
 * POST /functions/v1/ai-weekly-insight
 * Authorization: Bearer <user-jwt>
 * Body:
 *   {
 *     "week_start": "2026-03-30",
 *     "summary": {
 *       "avg_screen_time_hours": 3.2,
 *       "prev_week_screen_time_hours": 4.1,
 *       "focus_points_earned": 320,
 *       "streak_days": 6,
 *       "intent_honor_rate": 0.82,
 *       "top_struggle_times": ["21:00-23:00", "06:00-07:00"],
 *       "analog_activities_completed": 5,
 *       "emotional_trend": "improving",  // "improving" | "stable" | "declining"
 *       "biggest_win": "Kept phone-free mornings for 5 days straight",
 *       "focus_area": "Evening wind-down routine"  // user-chosen focus for next week
 *     }
 *   }
 *
 * Response 200:
 *   {
 *     "insight": "This week you made meaningful progress...",
 *     "week_start": "2026-03-30",
 *     "generated_at": "<iso-timestamp>",
 *     "model": "claude-sonnet-4-20250514",
 *     "cached": false
 *   }
 *
 * Response 429:
 *   { "error": "Rate limited", "next_eligible_at": "2026-04-06T00:00:00Z" }
 */

import { createClient } from "npm:@supabase/supabase-js@2";

const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers":
    "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

const ANTHROPIC_MODEL = "claude-sonnet-4-20250514";
const ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages";
const MAX_TOKENS = 512;

function getISOWeekStart(): string {
  const now = new Date();
  const day = now.getUTCDay();
  const diff = day === 0 ? -6 : 1 - day;
  const monday = new Date(now);
  monday.setUTCDate(now.getUTCDate() + diff);
  return monday.toISOString().split("T")[0];
}

function getNextWeekStart(weekStart: string): string {
  const d = new Date(weekStart);
  d.setUTCDate(d.getUTCDate() + 7);
  return d.toISOString().split("T")[0];
}

function isValidDate(dateStr: string): boolean {
  return /^\d{4}-\d{2}-\d{2}$/.test(dateStr) && !isNaN(Date.parse(dateStr));
}

interface WeeklySummary {
  avg_screen_time_hours: number;
  prev_week_screen_time_hours?: number;
  focus_points_earned: number;
  streak_days: number;
  intent_honor_rate: number;
  top_struggle_times?: string[];
  analog_activities_completed?: number;
  emotional_trend?: "improving" | "stable" | "declining";
  biggest_win?: string;
  focus_area?: string;
}

/** Builds the Anthropic prompt from the user's anonymized summary. */
function buildPrompt(summary: WeeklySummary, weekStart: string): string {
  const screenChangeText = summary.prev_week_screen_time_hours != null
    ? `Previous week: ${summary.prev_week_screen_time_hours}h average. ` +
      `Change: ${(summary.avg_screen_time_hours - summary.prev_week_screen_time_hours).toFixed(1)}h.`
    : "";

  return `You are Bilbo, a compassionate AI wellness coach helping someone reclaim their focus and mental health by reducing phone addiction.

Below is an anonymized summary of the user's digital wellness for the week starting ${weekStart}. Write a warm, personalized, actionable weekly insight of exactly 200 words. Be encouraging but honest. Celebrate wins. Name 1-2 specific improvement opportunities. End with a concrete micro-habit to try next week. Do not use bullet points — write in natural paragraphs.

--- WEEKLY SUMMARY ---
Average daily screen time: ${summary.avg_screen_time_hours} hours. ${screenChangeText}
Focus Points earned: ${summary.focus_points_earned} FP
Current streak: ${summary.streak_days} days
Intent honor rate: ${Math.round(summary.intent_honor_rate * 100)}% (declared intent matched actual behavior)
${summary.top_struggle_times?.length ? `Highest-risk time windows: ${summary.top_struggle_times.join(", ")}` : ""}
${summary.analog_activities_completed != null ? `Analog activities completed: ${summary.analog_activities_completed}` : ""}
${summary.emotional_trend ? `Emotional trend: ${summary.emotional_trend}` : ""}
${summary.biggest_win ? `Biggest win this week: "${summary.biggest_win}"` : ""}
${summary.focus_area ? `User's chosen focus area for next week: "${summary.focus_area}"` : ""}
--- END SUMMARY ---

Write the 200-word insight now:`;
}

/** Calls the Anthropic Messages API and returns the generated text. */
async function callAnthropicAPI(
  prompt: string,
  apiKey: string,
): Promise<string> {
  const response = await fetch(ANTHROPIC_API_URL, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "x-api-key": apiKey,
      "anthropic-version": "2023-06-01",
    },
    body: JSON.stringify({
      model: ANTHROPIC_MODEL,
      max_tokens: MAX_TOKENS,
      messages: [
        { role: "user", content: prompt },
      ],
    }),
  });

  if (!response.ok) {
    const errText = await response.text();
    throw new Error(`Anthropic API error ${response.status}: ${errText}`);
  }

  const data = await response.json();
  const content = data.content?.[0];
  if (!content || content.type !== "text") {
    throw new Error("Unexpected Anthropic API response format");
  }

  return content.text.trim();
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

  let body: { week_start?: string; summary?: Partial<WeeklySummary> };
  try {
    body = await req.json();
  } catch {
    return new Response(
      JSON.stringify({ error: "Invalid JSON body" }),
      { status: 400, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  const { week_start, summary } = body;

  if (!week_start || !isValidDate(week_start)) {
    return new Response(
      JSON.stringify({ error: "Field 'week_start' must be a valid ISO date (YYYY-MM-DD)" }),
      { status: 400, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  if (!summary || typeof summary !== "object") {
    return new Response(
      JSON.stringify({ error: "Field 'summary' is required and must be an object" }),
      { status: 400, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  // Validate required summary fields
  if (typeof summary.avg_screen_time_hours !== "number" || summary.avg_screen_time_hours < 0) {
    return new Response(
      JSON.stringify({ error: "summary.avg_screen_time_hours must be a non-negative number" }),
      { status: 400, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }
  if (typeof summary.focus_points_earned !== "number") {
    return new Response(
      JSON.stringify({ error: "summary.focus_points_earned is required" }),
      { status: 400, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const supabaseAnonKey = Deno.env.get("SUPABASE_ANON_KEY");
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  const anthropicApiKey = Deno.env.get("ANTHROPIC_API_KEY");

  if (!supabaseUrl || !supabaseAnonKey || !serviceRoleKey || !anthropicApiKey) {
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

  const supabaseAdmin = createClient(supabaseUrl, serviceRoleKey);

  // Check rate limit: 1 insight per user per week
  const { data: existingInsight } = await supabaseAdmin
    .from("ai_insights")
    .select("id, insight, generated_at")
    .eq("user_id", user.id)
    .eq("week_start", week_start)
    .maybeSingle();

  if (existingInsight) {
    // Return cached insight
    return new Response(
      JSON.stringify({
        insight: existingInsight.insight,
        week_start,
        generated_at: existingInsight.generated_at,
        model: ANTHROPIC_MODEL,
        cached: true,
      }),
      {
        status: 200,
        headers: { ...CORS_HEADERS, "Content-Type": "application/json" },
      },
    );
  }

  // Enforce: only allow insight for the current or previous week
  const currentWeekStart = getISOWeekStart();
  const previousWeekStart = (() => {
    const d = new Date(currentWeekStart);
    d.setUTCDate(d.getUTCDate() - 7);
    return d.toISOString().split("T")[0];
  })();

  if (week_start !== currentWeekStart && week_start !== previousWeekStart) {
    return new Response(
      JSON.stringify({ error: "Insights can only be generated for the current or previous week" }),
      { status: 400, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  // Check rate limit table: hard cap of 1 generation per user per week
  const { data: rateLimitRecord } = await supabaseAdmin
    .from("ai_insight_rate_limits")
    .select("used_at")
    .eq("user_id", user.id)
    .eq("week_start", week_start)
    .maybeSingle();

  if (rateLimitRecord) {
    const nextEligibleAt = `${getNextWeekStart(week_start)}T00:00:00Z`;
    return new Response(
      JSON.stringify({
        error: "Rate limited: AI insight already generated for this week",
        next_eligible_at: nextEligibleAt,
      }),
      {
        status: 429,
        headers: {
          ...CORS_HEADERS,
          "Content-Type": "application/json",
          "Retry-After": String(Math.ceil((new Date(nextEligibleAt).getTime() - Date.now()) / 1000)),
        },
      },
    );
  }

  // Call Anthropic API
  const prompt = buildPrompt(summary as WeeklySummary, week_start);
  let insightText: string;
  try {
    insightText = await callAnthropicAPI(prompt, anthropicApiKey);
  } catch (err) {
    console.error("ai-weekly-insight: Anthropic API error", err);
    return new Response(
      JSON.stringify({ error: "Failed to generate insight. Please try again later." }),
      { status: 502, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  const generatedAt = new Date().toISOString();

  // Persist the insight
  const { error: insightInsertError } = await supabaseAdmin
    .from("ai_insights")
    .insert({
      user_id: user.id,
      week_start,
      insight: insightText,
      model: ANTHROPIC_MODEL,
      generated_at: generatedAt,
    });

  if (insightInsertError) {
    console.error("ai-weekly-insight: insight insert error", insightInsertError);
    // Still return the insight even if caching fails
  }

  // Record rate limit usage
  await supabaseAdmin
    .from("ai_insight_rate_limits")
    .insert({ user_id: user.id, week_start, used_at: generatedAt });

  return new Response(
    JSON.stringify({
      insight: insightText,
      week_start,
      generated_at: generatedAt,
      model: ANTHROPIC_MODEL,
      cached: false,
    }),
    {
      status: 200,
      headers: { ...CORS_HEADERS, "Content-Type": "application/json" },
    },
  );
});
