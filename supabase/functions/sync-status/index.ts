/**
 * sync-status — Spark Edge Function
 *
 * Receives a compact status summary from the device and upserts it into
 * the `status_summaries` table. Called periodically by the mobile app
 * (e.g., when the app is foregrounded or when a usage session ends).
 *
 * POST /functions/v1/sync-status
 * Authorization: Bearer <user-jwt>
 * Body:
 *   {
 *     "date": "2026-04-06",                   // ISO date (device local date)
 *     "total_screen_time_seconds": 7200,
 *     "focus_points_earned": 42,
 *     "focus_points_spent": 10,
 *     "streak_days": 5,
 *     "intent_sessions_total": 12,
 *     "intent_sessions_honored": 9,
 *     "emotional_avg_mood": 3.5,              // 1–5 scale, nullable
 *     "emotional_avg_energy": 2.8,            // 1–5 scale, nullable
 *     "analog_time_seconds": 3600,
 *     "top_apps": [                           // top 3 apps by usage, no PII
 *       { "package": "com.example.app", "seconds": 1800 }
 *     ]
 *   }
 *
 * Response 200:
 *   { "synced": true, "summary_id": "<uuid>" }
 */

import { createClient } from "npm:@supabase/supabase-js@2";

const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers":
    "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

interface StatusSummaryPayload {
  date: string;
  total_screen_time_seconds: number;
  focus_points_earned: number;
  focus_points_spent: number;
  streak_days: number;
  intent_sessions_total: number;
  intent_sessions_honored: number;
  emotional_avg_mood?: number | null;
  emotional_avg_energy?: number | null;
  analog_time_seconds: number;
  top_apps?: Array<{ package: string; seconds: number }>;
}

function isValidDate(dateStr: string): boolean {
  return /^\d{4}-\d{2}-\d{2}$/.test(dateStr) && !isNaN(Date.parse(dateStr));
}

function validatePayload(payload: Partial<StatusSummaryPayload>): string | null {
  if (!payload.date || !isValidDate(payload.date)) {
    return "Field 'date' must be a valid ISO date string (YYYY-MM-DD)";
  }
  const numericFields: (keyof StatusSummaryPayload)[] = [
    "total_screen_time_seconds",
    "focus_points_earned",
    "focus_points_spent",
    "streak_days",
    "intent_sessions_total",
    "intent_sessions_honored",
    "analog_time_seconds",
  ];
  for (const field of numericFields) {
    const val = payload[field];
    if (val === undefined || val === null || typeof val !== "number" || val < 0) {
      return `Field '${field}' must be a non-negative number`;
    }
  }
  if (
    payload.emotional_avg_mood !== undefined &&
    payload.emotional_avg_mood !== null &&
    (payload.emotional_avg_mood < 1 || payload.emotional_avg_mood > 5)
  ) {
    return "Field 'emotional_avg_mood' must be between 1 and 5";
  }
  if (
    payload.emotional_avg_energy !== undefined &&
    payload.emotional_avg_energy !== null &&
    (payload.emotional_avg_energy < 1 || payload.emotional_avg_energy > 5)
  ) {
    return "Field 'emotional_avg_energy' must be between 1 and 5";
  }
  return null;
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

  let body: Partial<StatusSummaryPayload>;
  try {
    body = await req.json();
  } catch {
    return new Response(
      JSON.stringify({ error: "Invalid JSON body" }),
      { status: 400, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  const validationError = validatePayload(body);
  if (validationError) {
    return new Response(
      JSON.stringify({ error: validationError }),
      { status: 400, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const supabaseAnonKey = Deno.env.get("SUPABASE_ANON_KEY");

  if (!supabaseUrl || !supabaseAnonKey) {
    return new Response(
      JSON.stringify({ error: "Server configuration error" }),
      { status: 500, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  const supabase = createClient(supabaseUrl, supabaseAnonKey, {
    global: { headers: { Authorization: authHeader } },
  });

  const { data: { user }, error: authError } = await supabase.auth.getUser();
  if (authError || !user) {
    return new Response(
      JSON.stringify({ error: "Unauthorized" }),
      { status: 401, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  // Upsert status summary (unique on user_id + date)
  const upsertData = {
    user_id: user.id,
    date: body.date,
    total_screen_time_seconds: body.total_screen_time_seconds,
    focus_points_earned: body.focus_points_earned,
    focus_points_spent: body.focus_points_spent,
    streak_days: body.streak_days,
    intent_sessions_total: body.intent_sessions_total,
    intent_sessions_honored: body.intent_sessions_honored,
    emotional_avg_mood: body.emotional_avg_mood ?? null,
    emotional_avg_energy: body.emotional_avg_energy ?? null,
    analog_time_seconds: body.analog_time_seconds,
    top_apps: body.top_apps ?? [],
    synced_at: new Date().toISOString(),
  };

  const { data: summary, error: upsertError } = await supabase
    .from("status_summaries")
    .upsert(upsertData, { onConflict: "user_id,date" })
    .select("id")
    .single();

  if (upsertError || !summary) {
    console.error("sync-status: upsert error", upsertError);
    return new Response(
      JSON.stringify({ error: "Failed to sync status", detail: upsertError?.message }),
      { status: 500, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  return new Response(
    JSON.stringify({ synced: true, summary_id: summary.id }),
    {
      status: 200,
      headers: { ...CORS_HEADERS, "Content-Type": "application/json" },
    },
  );
});
