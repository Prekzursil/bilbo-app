/**
 * create-challenge — Bilbo Edge Function
 *
 * Creates a new Challenge linked to a Focus Circle. Only circle admins
 * or members can create challenges for their circle. The challenge becomes
 * visible to all circle members immediately.
 *
 * POST /functions/v1/create-challenge
 * Authorization: Bearer <user-jwt>
 * Body:
 *   {
 *     "circle_id": "<uuid>",
 *     "title": "No doom-scroll mornings",
 *     "description": "No social media before 9am for 7 days",  // optional
 *     "challenge_type": "streak" | "total_fp" | "analog_time" | "intent_honor",
 *     "target_value": 7,          // e.g. 7 days, 500 FP, 3600 seconds
 *     "start_date": "2026-04-07", // ISO date
 *     "end_date": "2026-04-14",   // ISO date, must be after start_date
 *     "reward_fp": 100            // Focus Points awarded on completion
 *   }
 *
 * Response 201:
 *   { "challenge_id": "<uuid>", "title": "No doom-scroll mornings" }
 */

import { createClient } from "npm:@supabase/supabase-js@2";

const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers":
    "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

const VALID_CHALLENGE_TYPES = ["streak", "total_fp", "analog_time", "intent_honor"] as const;
type ChallengeType = typeof VALID_CHALLENGE_TYPES[number];

function isValidDate(dateStr: string): boolean {
  return /^\d{4}-\d{2}-\d{2}$/.test(dateStr) && !isNaN(Date.parse(dateStr));
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

  let body: {
    circle_id?: string;
    title?: string;
    description?: string;
    challenge_type?: string;
    target_value?: number;
    start_date?: string;
    end_date?: string;
    reward_fp?: number;
  };
  try {
    body = await req.json();
  } catch {
    return new Response(
      JSON.stringify({ error: "Invalid JSON body" }),
      { status: 400, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  const {
    circle_id,
    title,
    description,
    challenge_type,
    target_value,
    start_date,
    end_date,
    reward_fp = 50,
  } = body;

  // Validate required fields
  if (!circle_id) {
    return new Response(
      JSON.stringify({ error: "Field 'circle_id' is required" }),
      { status: 400, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }
  if (!title || typeof title !== "string" || title.trim().length < 3) {
    return new Response(
      JSON.stringify({ error: "Field 'title' must be at least 3 characters" }),
      { status: 400, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }
  if (!challenge_type || !VALID_CHALLENGE_TYPES.includes(challenge_type as ChallengeType)) {
    return new Response(
      JSON.stringify({ error: `Field 'challenge_type' must be one of: ${VALID_CHALLENGE_TYPES.join(", ")}` }),
      { status: 400, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }
  if (typeof target_value !== "number" || target_value <= 0) {
    return new Response(
      JSON.stringify({ error: "Field 'target_value' must be a positive number" }),
      { status: 400, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }
  if (!start_date || !isValidDate(start_date)) {
    return new Response(
      JSON.stringify({ error: "Field 'start_date' must be a valid ISO date (YYYY-MM-DD)" }),
      { status: 400, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }
  if (!end_date || !isValidDate(end_date)) {
    return new Response(
      JSON.stringify({ error: "Field 'end_date' must be a valid ISO date (YYYY-MM-DD)" }),
      { status: 400, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }
  if (new Date(end_date) <= new Date(start_date)) {
    return new Response(
      JSON.stringify({ error: "Field 'end_date' must be after 'start_date'" }),
      { status: 400, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }
  if (typeof reward_fp !== "number" || reward_fp < 0 || reward_fp > 10000) {
    return new Response(
      JSON.stringify({ error: "Field 'reward_fp' must be between 0 and 10000" }),
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

  // Verify user is a member of the circle
  const { data: membership } = await supabaseAdmin
    .from("circle_members")
    .select("role")
    .eq("circle_id", circle_id)
    .eq("user_id", user.id)
    .maybeSingle();

  if (!membership) {
    return new Response(
      JSON.stringify({ error: "You are not a member of this circle" }),
      { status: 403, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  // Verify the circle exists and is active
  const { data: circle } = await supabaseAdmin
    .from("circles")
    .select("id, is_active")
    .eq("id", circle_id)
    .maybeSingle();

  if (!circle || !circle.is_active) {
    return new Response(
      JSON.stringify({ error: "Circle not found or is no longer active" }),
      { status: 404, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  // Create the challenge
  const { data: challenge, error: challengeError } = await supabaseAdmin
    .from("challenges")
    .insert({
      circle_id,
      created_by: user.id,
      title: title.trim(),
      description: description?.trim() ?? null,
      challenge_type,
      target_value,
      start_date,
      end_date,
      reward_fp,
      status: "active",
    })
    .select("id, title")
    .single();

  if (challengeError || !challenge) {
    console.error("create-challenge: insert error", challengeError);
    return new Response(
      JSON.stringify({ error: "Failed to create challenge", detail: challengeError?.message }),
      { status: 500, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  return new Response(
    JSON.stringify({ challenge_id: challenge.id, title: challenge.title }),
    {
      status: 201,
      headers: { ...CORS_HEADERS, "Content-Type": "application/json" },
    },
  );
});
