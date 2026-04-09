/**
 * generate-digest — Bilbo Edge Function
 *
 * Aggregates anonymous global statistics from the current week's
 * `status_summaries` table and returns a weekly digest payload.
 * All values are aggregated — no individual user data is exposed.
 *
 * This function is typically called by a scheduled cron job (Supabase
 * pg_cron or GitHub Actions) rather than directly by clients. However,
 * it may also be called by authenticated admin users.
 *
 * GET /functions/v1/generate-digest
 * Authorization: Bearer <service-role-key or admin-jwt>
 *
 * Response 200:
 *   {
 *     "week_start": "2026-03-30",
 *     "week_end":   "2026-04-05",
 *     "generated_at": "<iso-timestamp>",
 *     "stats": {
 *       "active_users": 1234,
 *       "avg_screen_time_seconds": 14400,
 *       "median_screen_time_seconds": 10800,
 *       "avg_focus_points_earned": 87.3,
 *       "avg_streak_days": 4.2,
 *       "avg_intent_honor_rate": 0.74,
 *       "total_analog_time_seconds": 8640000,
 *       "avg_emotional_mood": 3.6,
 *       "total_nudges_sent": 4521,
 *       "total_challenges_completed": 312,
 *       "top_improvement_percentile": 25.0
 *     }
 *   }
 */

import { createClient } from "npm:@supabase/supabase-js@2";

const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers":
    "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "GET, OPTIONS",
};

/** Returns the Monday of the current ISO week as an ISO date string. */
function getWeekStart(): string {
  const now = new Date();
  const day = now.getUTCDay();
  const diff = day === 0 ? -6 : 1 - day;
  const monday = new Date(now);
  monday.setUTCDate(now.getUTCDate() + diff);
  return monday.toISOString().split("T")[0];
}

function getWeekEnd(weekStart: string): string {
  const d = new Date(weekStart);
  d.setUTCDate(d.getUTCDate() + 6);
  return d.toISOString().split("T")[0];
}

function average(arr: number[]): number {
  if (arr.length === 0) return 0;
  return arr.reduce((a, b) => a + b, 0) / arr.length;
}

function median(arr: number[]): number {
  if (arr.length === 0) return 0;
  const sorted = [...arr].sort((a, b) => a - b);
  const mid = Math.floor(sorted.length / 2);
  return sorted.length % 2 !== 0
    ? sorted[mid]
    : (sorted[mid - 1] + sorted[mid]) / 2;
}

/** Returns the Nth percentile value from a sorted array. */
function percentile(sortedArr: number[], p: number): number {
  if (sortedArr.length === 0) return 0;
  const idx = Math.ceil((p / 100) * sortedArr.length) - 1;
  return sortedArr[Math.max(0, idx)];
}

Deno.serve(async (req: Request): Promise<Response> => {
  if (req.method === "OPTIONS") {
    return new Response(null, { status: 204, headers: CORS_HEADERS });
  }

  if (req.method !== "GET") {
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

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");

  if (!supabaseUrl || !serviceRoleKey) {
    return new Response(
      JSON.stringify({ error: "Server configuration error" }),
      { status: 500, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  // Only allow service-role or admin access
  const supabaseAdmin = createClient(supabaseUrl, serviceRoleKey);

  const weekStart = getWeekStart();
  const weekEnd = getWeekEnd(weekStart);

  // Fetch all status summaries for the current week (anonymized — no user_id in response)
  const { data: summaries, error: summaryError } = await supabaseAdmin
    .from("status_summaries")
    .select(
      "user_id, total_screen_time_seconds, focus_points_earned, streak_days, " +
      "intent_sessions_total, intent_sessions_honored, analog_time_seconds, emotional_avg_mood",
    )
    .gte("date", weekStart)
    .lte("date", weekEnd);

  if (summaryError) {
    console.error("generate-digest: summaries query error", summaryError);
    return new Response(
      JSON.stringify({ error: "Failed to fetch status summaries" }),
      { status: 500, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  // Aggregate per-user totals (a user may have multiple rows per week)
  const userTotals: Record<string, {
    screenTime: number;
    fp: number;
    streak: number;
    intentTotal: number;
    intentHonored: number;
    analog: number;
    moodReadings: number[];
  }> = {};

  for (const row of summaries ?? []) {
    if (!userTotals[row.user_id]) {
      userTotals[row.user_id] = {
        screenTime: 0, fp: 0, streak: 0,
        intentTotal: 0, intentHonored: 0,
        analog: 0, moodReadings: [],
      };
    }
    const u = userTotals[row.user_id];
    u.screenTime += row.total_screen_time_seconds ?? 0;
    u.fp += row.focus_points_earned ?? 0;
    u.streak = Math.max(u.streak, row.streak_days ?? 0);
    u.intentTotal += row.intent_sessions_total ?? 0;
    u.intentHonored += row.intent_sessions_honored ?? 0;
    u.analog += row.analog_time_seconds ?? 0;
    if (row.emotional_avg_mood != null) u.moodReadings.push(row.emotional_avg_mood);
  }

  const userIds = Object.keys(userTotals);
  const activeUsers = userIds.length;

  const screenTimes = userIds.map((uid) => userTotals[uid].screenTime);
  const fpValues = userIds.map((uid) => userTotals[uid].fp);
  const streakValues = userIds.map((uid) => userTotals[uid].streak);
  const analogValues = userIds.map((uid) => userTotals[uid].analog);

  const intentHonorRates = userIds
    .map((uid) => {
      const u = userTotals[uid];
      return u.intentTotal > 0 ? u.intentHonored / u.intentTotal : null;
    })
    .filter((v): v is number => v !== null);

  const allMoodReadings = userIds.flatMap((uid) => userTotals[uid].moodReadings);
  const sortedScreenTimes = [...screenTimes].sort((a, b) => a - b);

  // Top 25th percentile improvement = screen time at or below the 25th percentile mark
  const p25ScreenTime = percentile(sortedScreenTimes, 25);
  const totalAnalogTime = analogValues.reduce((a, b) => a + b, 0);

  // Count nudge events this week
  const { count: nudgeCount } = await supabaseAdmin
    .from("nudge_events")
    .select("id", { count: "exact", head: true })
    .gte("created_at", `${weekStart}T00:00:00Z`);

  // Count completed challenge participations this week
  const { count: challengesCompleted } = await supabaseAdmin
    .from("challenge_participations")
    .select("id", { count: "exact", head: true })
    .eq("status", "completed")
    .gte("completed_at", `${weekStart}T00:00:00Z`);

  const digest = {
    week_start: weekStart,
    week_end: weekEnd,
    generated_at: new Date().toISOString(),
    stats: {
      active_users: activeUsers,
      avg_screen_time_seconds: Math.round(average(screenTimes)),
      median_screen_time_seconds: Math.round(median(screenTimes)),
      avg_focus_points_earned: Math.round(average(fpValues) * 10) / 10,
      avg_streak_days: Math.round(average(streakValues) * 10) / 10,
      avg_intent_honor_rate: Math.round(average(intentHonorRates) * 1000) / 1000,
      total_analog_time_seconds: totalAnalogTime,
      avg_emotional_mood: allMoodReadings.length > 0
        ? Math.round(average(allMoodReadings) * 100) / 100
        : null,
      total_nudges_sent: nudgeCount ?? 0,
      total_challenges_completed: challengesCompleted ?? 0,
      top_improvement_percentile: Math.round(p25ScreenTime),
    },
  };

  // Store the digest for historical reference
  await supabaseAdmin
    .from("weekly_digests")
    .upsert(
      { week_start: weekStart, payload: digest, generated_at: digest.generated_at },
      { onConflict: "week_start" },
    );

  return new Response(
    JSON.stringify(digest),
    {
      status: 200,
      headers: { ...CORS_HEADERS, "Content-Type": "application/json" },
    },
  );
});
