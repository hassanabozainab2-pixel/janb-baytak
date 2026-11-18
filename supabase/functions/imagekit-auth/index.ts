import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "npm:@supabase/supabase-js@2";

const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};
const json = (body: unknown, status = 200) => new Response(JSON.stringify(body), {
  status,
  headers: { ...cors, "Content-Type": "application/json" },
});

const MAX_FILE_BYTES = 50 * 1024 * 1024;
const ALLOWED_TYPES = new Set([
  "image/jpeg", "image/png", "image/webp",
  "video/mp4", "video/webm", "video/quicktime", "video/3gpp",
]);

function hex(bytes: Uint8Array) {
  return Array.from(bytes, (b) => b.toString(16).padStart(2, "0")).join("");
}

async function signUpload(token: string, expire: number, privateKey: string) {
  const key = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(privateKey),
    { name: "HMAC", hash: "SHA-1" },
    false,
    ["sign"],
  );
  const signature = await crypto.subtle.sign(
    "HMAC",
    key,
    new TextEncoder().encode(token + expire),
  );
  return hex(new Uint8Array(signature));
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: cors });
  if (req.method !== "POST") return json({ error: "Method not allowed" }, 405);

  const authorization = req.headers.get("Authorization") || "";
  const accessToken = authorization.replace(/^Bearer\s+/i, "");
  if (!accessToken || accessToken === authorization) {
    return json({ error: "Authentication required" }, 401);
  }

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const publishable = Deno.env.get("SUPABASE_ANON_KEY") ??
    (Deno.env.get("SUPABASE_PUBLISHABLE_KEYS")
      ? JSON.parse(Deno.env.get("SUPABASE_PUBLISHABLE_KEYS")!)[ "default" ]
      : "");
  const privateKey = Deno.env.get("IMAGEKIT_PRIVATE_KEY");
  const publicKey = Deno.env.get("IMAGEKIT_PUBLIC_KEY");

  if (!supabaseUrl || !publishable) {
    return json({ error: "Supabase authentication configuration missing" }, 503);
  }
  if (!privateKey || !publicKey) {
    return json({ error: "ImageKit keys are not configured on the server yet" }, 503);
  }

  const userClient = createClient(supabaseUrl, publishable, {
    global: { headers: { Authorization: "Bearer " + accessToken } },
    auth: { persistSession: false, autoRefreshToken: false },
  });
  const { data: authData, error: authError } = await userClient.auth.getUser(accessToken);
  if (authError || !authData.user) return json({ error: "Valid login required" }, 401);

  let input: { fileName?: unknown; fileSize?: unknown; fileType?: unknown };
  try {
    input = await req.json();
  } catch {
    return json({ error: "Invalid JSON" }, 400);
  }

  const fileName = String(input.fileName || "").trim();
  const fileSize = Number(input.fileSize);
  const fileType = String(input.fileType || "").toLowerCase();

  if (!fileName || fileName.length > 180 || /[\\/]/.test(fileName)) {
    return json({ error: "Invalid file name" }, 400);
  }
  if (!Number.isFinite(fileSize) || fileSize <= 0 || fileSize > MAX_FILE_BYTES) {
    return json({ error: "File must be between 1 byte and 50 MB" }, 413);
  }
  if (!ALLOWED_TYPES.has(fileType)) {
    return json({ error: "This image or video format is not supported" }, 415);
  }

  const token = crypto.randomUUID();
  const expire = Math.floor(Date.now() / 1000) + 600;
  try {
    const signature = await signUpload(token, expire, privateKey);
    return json({
      token,
      expire,
      signature,
      publicKey,
      userId: authData.user.id,
      maxFileSizeBytes: MAX_FILE_BYTES,
    });
  } catch (error) {
    console.error("ImageKit signature generation failed", error);
    return json({ error: "Could not prepare a secure upload" }, 500);
  }
});
