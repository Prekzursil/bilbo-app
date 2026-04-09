/**
 * accept-invite — Bilbo Edge Function
 *
 * Validates a buddy invite code, checks expiry, creates a buddy_pair record,
 * and marks the invite code as used.
 *
 * POST /functions/v1/accept-invite
 * Authorization: Bearer <user-jwt>
 * Body: { "code": "A1B2C3" }
 *
 * Response 200:
 *   { "buddy_pair_id": "<uuid>", "buddy_user_id": "<uuid>" }
 *
 * Error responses:
 *   400 — missing/invalid body
 *   401 — unauthenticated
 *   404 — code not found
 *   409 — code already used, expired, or user is the code creator
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

  // Parse and validate request body
  let body: { code?: string };
  try {
    body = await req.json();
  } catch {
    return new Response(
      JSON.stringify({ error: "Invalid JSON body" }),
      { status: 400, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  const { code } = body;
  if (!code || typeof code !== "string" || code.trim().length === 0) {
    return new Response(
      JSON.stringify({ error: "Field 'code' is required" }),
      { status: 400, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  const normalizedCode = code.trim().toUpperCase();

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const supabaseAnonKey = Deno.env.get("SUPABASE_ANON_KEY");
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");

  if (!supabaseUrl || !supabaseAnonKey || !serviceRoleKey) {
    return new Response(
      JSON.stringify({ error: "Server configuration error" }),
      { status: 500, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  // User-scoped client for identity verification
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

  // Service-role client for privileged mutations
  const supabaseAdmin = createClient(supabaseUrl, serviceRoleKey);

  // Look up the invite code
  const { data: invite, error: lookupError } = await supabaseAdmin
    .from("invite_codes")
    .select("id, code, created_by, expires_at, used")
    .eq("code", normalizedCode)
    .maybeSingle();

  if (lookupError) {
    console.error("accept-invite: lookup error", lookupError);
    return new Response(
      JSON.stringify({ error: "Failed to look up invite code" }),
      { status: 500, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  if (!invite) {
    return new Response(
      JSON.stringify({ error: "Invite code not found" }),
      { status: 404, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  if (invite.used) {
    return new Response(
      JSON.stringify({ error: "Invite code has already been used" }),
      { status: 409, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  if (new Date(invite.expires_at) < new Date()) {
    return new Response(
      JSON.stringify({ error: "Invite code has expired" }),
      { status: 409, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  if (invite.created_by === user.id) {
    return new Response(
      JSON.stringify({ error: "You cannot accept your own invite code" }),
      { status: 409, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  // Check if a buddy_pair already exists between these two users
  const { data: existingPair } = await supabaseAdmin
    .from("buddy_pairs")
    .select("id")
    .or(
      `and(user_a.eq.${user.id},user_b.eq.${invite.created_by}),and(user_a.eq.${invite.created_by},user_b.eq.${user.id})`,
    )
    .maybeSingle();

  if (existingPair) {
    return new Response(
      JSON.stringify({ error: "You are already buddies with this user" }),
      { status: 409, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  // Create the buddy pair
  const { data: buddyPair, error: pairError } = await supabaseAdmin
    .from("buddy_pairs")
    .insert({
      user_a: invite.created_by,
      user_b: user.id,
      created_at: new Date().toISOString(),
    })
    .select("id")
    .single();

  if (pairError || !buddyPair) {
    console.error("accept-invite: buddy_pair insert error", pairError);
    return new Response(
      JSON.stringify({ error: "Failed to create buddy pair" }),
      { status: 500, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  // Mark the invite code as used
  const { error: markUsedError } = await supabaseAdmin
    .from("invite_codes")
    .update({ used: true, used_by: user.id, used_at: new Date().toISOString() })
    .eq("id", invite.id);

  if (markUsedError) {
    console.error("accept-invite: failed to mark code as used", markUsedError);
    // Not fatal — pair was created successfully; log and continue
  }

  return new Response(
    JSON.stringify({
      buddy_pair_id: buddyPair.id,
      buddy_user_id: invite.created_by,
    }),
    {
      status: 200,
      headers: { ...CORS_HEADERS, "Content-Type": "application/json" },
    },
  );
});
