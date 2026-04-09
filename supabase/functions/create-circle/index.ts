/**
 * create-circle — Spark Edge Function
 *
 * Creates a new Focus Circle, generates a unique 8-character circle invite code,
 * and adds the creator as the first member with role 'admin'.
 *
 * POST /functions/v1/create-circle
 * Authorization: Bearer <user-jwt>
 * Body:
 *   {
 *     "name": "Morning Warriors",
 *     "description": "We don't touch phones until 9am.",  // optional
 *     "max_members": 8,                                    // 2–20, default 8
 *     "is_public": false                                   // default false
 *   }
 *
 * Response 201:
 *   {
 *     "circle_id": "<uuid>",
 *     "invite_code": "MRN3WRR8",
 *     "name": "Morning Warriors"
 *   }
 */

import { createClient } from "npm:@supabase/supabase-js@2";

const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers":
    "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

const INVITE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
const INVITE_CODE_LENGTH = 8;
const DEFAULT_MAX_MEMBERS = 8;
const MIN_MAX_MEMBERS = 2;
const MAX_MAX_MEMBERS = 20;

function generateCircleCode(): string {
  const bytes = new Uint8Array(INVITE_CODE_LENGTH);
  crypto.getRandomValues(bytes);
  return Array.from(bytes)
    .map((b) => INVITE_ALPHABET[b % INVITE_ALPHABET.length])
    .join("");
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
    name?: string;
    description?: string;
    max_members?: number;
    is_public?: boolean;
  };
  try {
    body = await req.json();
  } catch {
    return new Response(
      JSON.stringify({ error: "Invalid JSON body" }),
      { status: 400, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  const { name, description, max_members = DEFAULT_MAX_MEMBERS, is_public = false } = body;

  if (!name || typeof name !== "string" || name.trim().length < 3) {
    return new Response(
      JSON.stringify({ error: "Field 'name' must be at least 3 characters" }),
      { status: 400, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  if (name.trim().length > 60) {
    return new Response(
      JSON.stringify({ error: "Field 'name' must be at most 60 characters" }),
      { status: 400, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  if (
    typeof max_members !== "number" ||
    max_members < MIN_MAX_MEMBERS ||
    max_members > MAX_MAX_MEMBERS
  ) {
    return new Response(
      JSON.stringify({ error: `Field 'max_members' must be between ${MIN_MAX_MEMBERS} and ${MAX_MAX_MEMBERS}` }),
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

  // Generate a unique circle invite code
  let inviteCode = "";
  let circleRecord = null;

  for (let attempt = 0; attempt < 5; attempt++) {
    inviteCode = generateCircleCode();

    const { data, error: insertError } = await supabaseAdmin
      .from("circles")
      .insert({
        name: name.trim(),
        description: description?.trim() ?? null,
        invite_code: inviteCode,
        max_members,
        is_public,
        created_by: user.id,
      })
      .select("id, name, invite_code")
      .single();

    if (!insertError) {
      circleRecord = data;
      break;
    }

    // Unique constraint violation on invite_code — retry
    if (insertError.code !== "23505") {
      console.error("create-circle: insert error", insertError);
      return new Response(
        JSON.stringify({ error: "Failed to create circle", detail: insertError.message }),
        { status: 500, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
      );
    }
  }

  if (!circleRecord) {
    return new Response(
      JSON.stringify({ error: "Failed to generate a unique circle code after retries" }),
      { status: 500, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  // Add creator as first member with role 'admin'
  const { error: memberError } = await supabaseAdmin
    .from("circle_members")
    .insert({
      circle_id: circleRecord.id,
      user_id: user.id,
      role: "admin",
      joined_at: new Date().toISOString(),
    });

  if (memberError) {
    console.error("create-circle: member insert error", memberError);
    // Clean up the circle to maintain consistency
    await supabaseAdmin.from("circles").delete().eq("id", circleRecord.id);
    return new Response(
      JSON.stringify({ error: "Failed to add creator as circle member" }),
      { status: 500, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  return new Response(
    JSON.stringify({
      circle_id: circleRecord.id,
      invite_code: circleRecord.invite_code,
      name: circleRecord.name,
    }),
    {
      status: 201,
      headers: { ...CORS_HEADERS, "Content-Type": "application/json" },
    },
  );
});
