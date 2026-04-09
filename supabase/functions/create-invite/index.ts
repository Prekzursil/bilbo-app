/**
 * create-invite — Bilbo Edge Function
 *
 * Generates a 6-character alphanumeric invite code, stores it in the
 * `invite_codes` table with a 24-hour expiry, and returns the code to the caller.
 *
 * POST /functions/v1/create-invite
 * Authorization: Bearer <user-jwt>
 *
 * Response 201:
 *   { "code": "A1B2C3", "expires_at": "2026-04-07T12:00:00Z" }
 */

import { createClient } from "npm:@supabase/supabase-js@2";

const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers":
    "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

const CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // omit I, O, 0, 1 for clarity
const CODE_LENGTH = 6;
const EXPIRY_HOURS = 24;

/** Generates a cryptographically random alphanumeric invite code. */
function generateInviteCode(): string {
  const bytes = new Uint8Array(CODE_LENGTH);
  crypto.getRandomValues(bytes);
  return Array.from(bytes)
    .map((b) => CODE_ALPHABET[b % CODE_ALPHABET.length])
    .join("");
}

Deno.serve(async (req: Request): Promise<Response> => {
  // Handle CORS preflight
  if (req.method === "OPTIONS") {
    return new Response(null, { status: 204, headers: CORS_HEADERS });
  }

  if (req.method !== "POST") {
    return new Response(
      JSON.stringify({ error: "Method not allowed" }),
      { status: 405, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  // Extract JWT from Authorization header
  const authHeader = req.headers.get("Authorization");
  if (!authHeader?.startsWith("Bearer ")) {
    return new Response(
      JSON.stringify({ error: "Missing or invalid Authorization header" }),
      { status: 401, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
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

  // Create a client scoped to the requesting user's JWT
  const supabase = createClient(supabaseUrl, supabaseAnonKey, {
    global: { headers: { Authorization: authHeader } },
  });

  // Retrieve the authenticated user
  const { data: { user }, error: authError } = await supabase.auth.getUser();
  if (authError || !user) {
    return new Response(
      JSON.stringify({ error: "Unauthorized" }),
      { status: 401, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  // Generate a unique invite code (retry up to 5 times on collision)
  let code = "";
  let inserted = false;
  const expiresAt = new Date(Date.now() + EXPIRY_HOURS * 60 * 60 * 1000).toISOString();

  for (let attempt = 0; attempt < 5; attempt++) {
    code = generateInviteCode();

    const { error: insertError } = await supabase
      .from("invite_codes")
      .insert({
        code,
        created_by: user.id,
        expires_at: expiresAt,
        used: false,
      });

    if (!insertError) {
      inserted = true;
      break;
    }

    // Code collision (unique constraint violation) — retry with a new code
    if (insertError.code !== "23505") {
      console.error("create-invite: insert error", insertError);
      return new Response(
        JSON.stringify({ error: "Failed to create invite code", detail: insertError.message }),
        { status: 500, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
      );
    }
  }

  if (!inserted) {
    return new Response(
      JSON.stringify({ error: "Failed to generate a unique invite code after retries" }),
      { status: 500, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }

  return new Response(
    JSON.stringify({ code, expires_at: expiresAt }),
    {
      status: 201,
      headers: { ...CORS_HEADERS, "Content-Type": "application/json" },
    },
  );
});
