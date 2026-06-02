/** Optional shared secret — set STRIPE_BACKEND_API_KEY in function secrets. */
export function assertBackendAuth(req: Request): Response | null {
  const expected = Deno.env.get("STRIPE_BACKEND_API_KEY")?.trim();
  if (!expected) return null;

  const header = req.headers.get("x-stripe-backend-key")?.trim() ??
    req.headers.get("authorization")?.replace(/^Bearer\s+/i, "").trim();

  if (header !== expected) {
    return new Response(JSON.stringify({ error: "Unauthorized" }), {
      status: 401,
      headers: { "Content-Type": "application/json" },
    });
  }
  return null;
}
