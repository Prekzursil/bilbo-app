/**
 * join-circle — Bilbo Edge Function
 *
 * Validates a circle invite code, checks that the circle is not full,
 * and adds the authenticated user as a member.
 *
 * POST /functions/v1/join-circle
 * Authorization: Bearer <user-jwt>
 * Body: { "invite_code": "MRN3WRR8" }
 *
 * Response 200:
 *   {
 *     "circle_id": "<uuid>",
 *     "circle_name": "Morning Warriors",
 *     "member_count": 3
 *   }
 *
 * Error responses:
 *   400 — missing invite_code
 *   401 — unauthenticated
 *   404 — circle not found
 *   409 — circle full, already a member, or circle is inactive
 *   500 — server error
 */

import { createClient } from "npm:@supabase/supabase-js@2";

const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers":
    "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

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

  let body: { invite_code?: string };
  try {
    body = await req.json();
  } catch {
    return new Response(
      JSON.stringify({ error: "Invalid JSON body" }),
      { status: 400, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  const { invite_code } = body;
  if (!invite_code || typeof invite_code !== "string" || invite_code.trim().length === 0) {
    return new Response(
      JSON.stringify({ error: "Field 'invite_code' is required" }),
      { status: 400, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  const normalizedCode = invite_code.trim().toUpperCase();

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

  // Look up the circle by invite code
  const { data: circle, error: circleLookupError } = await supabaseAdmin
    .from("circles")
    .select("id, name, max_members, is_active")
    .eq("invite_code", normalizedCode)
    .maybeSingle();

  if (circleLookupError) {
    console.error("join-circle: lookup error", circleLookupError);
    return new Response(
      JSON.stringify({ error: "Failed to look up circle" }),
      { status: 500, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  if (!circle) {
    return new Response(
      JSON.stringify({ error: "Circle not found for the given invite code" }),
      { status: 404, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  if (!circle.is_active) {
    return new Response(
      JSON.stringify({ error: "This circle is no longer active" }),
      { status: 409, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  // Count current members
  const { count: memberCount, error: countError } = await supabaseAdmin
    .from("circle_members")
    .select("id", { count: "exact", head: true })
    .eq("circle_id", circle.id);

  if (countError) {
    console.error("join-circle: count error", countError);
    return new Response(
      JSON.stringify({ error: "Failed to check circle capacity" }),
      { status: 500, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  if ((memberCount ?? 0) >= circle.max_members) {
    return new Response(
      JSON.stringify({
        error: `Circle is full (${circle.max_members} members maximum)`,
        max_members: circle.max_members,
      }),
      { status: 409, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  // Check if user is already a member
  const { data: existingMembership } = await supabaseAdmin
    .from("circle_members")
    .select("id")
    .eq("circle_id", circle.id)
    .eq("user_id", user.id)
    .maybeSingle();

  if (existingMembership) {
    return new Response(
      JSON.stringify({ error: "You are already a member of this circle" }),
      { status: 409, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  // Add user as a member with role 'member'
  const { error: joinError } = await supabaseAdmin
    .from("circle_members")
    .insert({
      circle_id: circle.id,
      user_id: user.id,
      role: "member",
      joined_at: new Date().toISOString(),
    });

  if (joinError) {
    console.error("join-circle: join error", joinError);
    return new Response(
      JSON.stringify({ error: "Failed to join circle" }),
      { status: 500, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  const newMemberCount = (memberCount ?? 0) + 1;

  return new Response(
    JSON.stringify({
      circle_id: circle.id,
      circle_name: circle.name,
      member_count: newMemberCount,
    }),
    {
      status: 200,
      headers: { ...CORS_HEADERS, "Content-Type": "application/json" },
    },
  );
});
