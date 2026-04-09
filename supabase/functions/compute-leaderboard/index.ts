/**
 * compute-leaderboard — Bilbo Edge Function
 *
 * Computes the leaderboard for a Focus Circle across four categories:
 *   - most_fp         : Highest total Focus Points earned this week
 *   - best_streak     : Longest current streak (days)
 *   - most_improved   : Biggest % reduction in screen time vs. previous week
 *   - most_analog     : Most analog activity time (seconds) this week
 *
 * Results are computed from the `status_summaries` table for the current
 * ISO week (Monday–Sunday). Results are cached in `leaderboard_cache` for
 * 5 minutes to avoid hammering the DB on rapid requests.
 *
 * GET /functions/v1/compute-leaderboard?circle_id=<uuid>
 * Authorization: Bearer <user-jwt>
 *
 * Response 200:
 *   {
 *     "circle_id": "<uuid>",
 *     "week_start": "2026-03-30",
 *     "computed_at": "<iso-timestamp>",
 *     "leaderboard": {
 *       "most_fp":       [{ "user_id": "…", "display_name": "…", "value": 420 }, …],
 *       "best_streak":   [{ "user_id": "…", "display_name": "…", "value": 14 }, …],
 *       "most_improved": [{ "user_id": "…", "display_name": "…", "value": 32.5 }, …],
 *       "most_analog":   [{ "user_id": "…", "display_name": "…", "value": 18000 }, …]
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

const CACHE_TTL_SECONDS = 300; // 5 minutes

/** Returns the Monday of the current ISO week as an ISO date string. */
function getWeekStart(): string {
  const now = new Date();
  const day = now.getUTCDay(); // 0 = Sun, 1 = Mon, …
  const diff = (day === 0 ? -6 : 1 - day);
  const monday = new Date(now);
  monday.setUTCDate(now.getUTCDate() + diff);
  return monday.toISOString().split("T")[0];
}

/** Returns the Sunday of the week containing weekStart. */
function getWeekEnd(weekStart: string): string {
  const d = new Date(weekStart);
  d.setUTCDate(d.getUTCDate() + 6);
  return d.toISOString().split("T")[0];
}

/** Returns the Monday of the previous week. */
function getPreviousWeekStart(weekStart: string): string {
  const d = new Date(weekStart);
  d.setUTCDate(d.getUTCDate() - 7);
  return d.toISOString().split("T")[0];
}

interface LeaderboardEntry {
  user_id: string;
  display_name: string;
  value: number;
}

interface LeaderboardResult {
  most_fp: LeaderboardEntry[];
  best_streak: LeaderboardEntry[];
  most_improved: LeaderboardEntry[];
  most_analog: LeaderboardEntry[];
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

  const url = new URL(req.url);
  const circleId = url.searchParams.get("circle_id");
  if (!circleId) {
    return new Response(
      JSON.stringify({ error: "Query parameter 'circle_id' is required" }),
      { status: 400, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const supabaseAnonKey = Deno.env.get("SUPABASE_ANON_KEY");
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");

  if (!supabaseUrl || !supabaseAnonKey || !serviceRoleKey) {
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

  // Verify the user is a member of the circle
  const { data: membership } = await supabaseAdmin
    .from("circle_members")
    .select("user_id")
    .eq("circle_id", circleId)
    .eq("user_id", user.id)
    .maybeSingle();

  if (!membership) {
    return new Response(
      JSON.stringify({ error: "You are not a member of this circle" }),
      { status: 403, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  const weekStart = getWeekStart();
  const cacheKey = `${circleId}:${weekStart}`;

  // Check leaderboard cache
  const { data: cached } = await supabaseAdmin
    .from("leaderboard_cache")
    .select("payload, computed_at")
    .eq("cache_key", cacheKey)
    .maybeSingle();

  if (cached) {
    const ageSeconds = (Date.now() - new Date(cached.computed_at).getTime()) / 1000;
    if (ageSeconds < CACHE_TTL_SECONDS) {
      return new Response(
        JSON.stringify(cached.payload),
        {
          status: 200,
          headers: { ...CORS_HEADERS, "Content-Type": "application/json", "X-Cache": "HIT" },
        },
      );
    }
  }

  const weekEnd = getWeekEnd(weekStart);
  const prevWeekStart = getPreviousWeekStart(weekStart);
  const prevWeekEnd = weekStart;

  // Fetch all circle member IDs
  const { data: members, error: membersError } = await supabaseAdmin
    .from("circle_members")
    .select("user_id")
    .eq("circle_id", circleId);

  if (membersError || !members || members.length === 0) {
    return new Response(
      JSON.stringify({ error: "Failed to fetch circle members" }),
      { status: 500, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  const memberIds = members.map((m) => m.user_id);

  // Fetch user display names
  const { data: profiles } = await supabaseAdmin
    .from("user_profiles")
    .select("user_id, display_name")
    .in("user_id", memberIds);

  const displayNameMap: Record<string, string> = {};
  for (const profile of profiles ?? []) {
    displayNameMap[profile.user_id] = profile.display_name ?? "Anonymous";
  }

  // Fetch current week status summaries
  const { data: currentWeekRows, error: currentError } = await supabaseAdmin
    .from("status_summaries")
    .select("user_id, focus_points_earned, streak_days, analog_time_seconds, total_screen_time_seconds")
    .in("user_id", memberIds)
    .gte("date", weekStart)
    .lte("date", weekEnd);

  if (currentError) {
    console.error("compute-leaderboard: current week query error", currentError);
    return new Response(
      JSON.stringify({ error: "Failed to compute leaderboard" }),
      { status: 500, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  // Fetch previous week status summaries for 'most_improved'
  const { data: prevWeekRows } = await supabaseAdmin
    .from("status_summaries")
    .select("user_id, total_screen_time_seconds")
    .in("user_id", memberIds)
    .gte("date", prevWeekStart)
    .lt("date", prevWeekEnd);

  // Aggregate current week stats per user
  const currentStats: Record<string, { fp: number; streak: number; analog: number; screen: number }> = {};
  for (const row of currentWeekRows ?? []) {
    if (!currentStats[row.user_id]) {
      currentStats[row.user_id] = { fp: 0, streak: 0, analog: 0, screen: 0 };
    }
    currentStats[row.user_id].fp += row.focus_points_earned ?? 0;
    currentStats[row.user_id].streak = Math.max(currentStats[row.user_id].streak, row.streak_days ?? 0);
    currentStats[row.user_id].analog += row.analog_time_seconds ?? 0;
    currentStats[row.user_id].screen += row.total_screen_time_seconds ?? 0;
  }

  // Aggregate previous week screen time per user
  const prevScreenTime: Record<string, number> = {};
  for (const row of prevWeekRows ?? []) {
    prevScreenTime[row.user_id] = (prevScreenTime[row.user_id] ?? 0) + (row.total_screen_time_seconds ?? 0);
  }

  function makeEntries(
    valueSelector: (userId: string) => number,
  ): LeaderboardEntry[] {
    return memberIds
      .map((uid) => ({
        user_id: uid,
        display_name: displayNameMap[uid] ?? "Anonymous",
        value: valueSelector(uid),
      }))
      .filter((e) => e.value > 0)
      .sort((a, b) => b.value - a.value)
      .slice(0, 10);
  }

  const mostImprovedEntries: LeaderboardEntry[] = memberIds
    .map((uid) => {
      const curr = currentStats[uid]?.screen ?? 0;
      const prev = prevScreenTime[uid] ?? 0;
      const improvement = prev > 0 ? ((prev - curr) / prev) * 100 : 0;
      return {
        user_id: uid,
        display_name: displayNameMap[uid] ?? "Anonymous",
        value: Math.max(0, Math.round(improvement * 10) / 10),
      };
    })
    .filter((e) => e.value > 0)
    .sort((a, b) => b.value - a.value)
    .slice(0, 10);

  const leaderboard: LeaderboardResult = {
    most_fp: makeEntries((uid) => currentStats[uid]?.fp ?? 0),
    best_streak: makeEntries((uid) => currentStats[uid]?.streak ?? 0),
    most_improved: mostImprovedEntries,
    most_analog: makeEntries((uid) => currentStats[uid]?.analog ?? 0),
  };

  const computedAt = new Date().toISOString();
  const responsePayload = {
    circle_id: circleId,
    week_start: weekStart,
    computed_at: computedAt,
    leaderboard,
  };

  // Upsert cache entry
  await supabaseAdmin
    .from("leaderboard_cache")
    .upsert({
      cache_key: cacheKey,
      circle_id: circleId,
      payload: responsePayload,
      computed_at: computedAt,
    }, { onConflict: "cache_key" });

  return new Response(
    JSON.stringify(responsePayload),
    {
      status: 200,
      headers: { ...CORS_HEADERS, "Content-Type": "application/json", "X-Cache": "MISS" },
    },
  );
});
