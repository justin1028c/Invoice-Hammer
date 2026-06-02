/** Shared gate key — set FOREMAN_BACKEND_API_KEY in function secrets (Phase A). */
export function assertForemanAuth(req: Request): Response | null {
  const expected = Deno.env.get("FOREMAN_BACKEND_API_KEY")?.trim();
  if (!expected) {
    return new Response(JSON.stringify({ error: "FOREMAN_BACKEND_API_KEY not configured" }), {
      status: 500,
      headers: { "Content-Type": "application/json" },
    });
  }

  const header = req.headers.get("x-foreman-backend-key")?.trim() ??
    req.headers.get("authorization")?.replace(/^Bearer\s+/i, "").trim();

  if (header !== expected) {
    return new Response(JSON.stringify({ error: "Unauthorized" }), {
      status: 401,
      headers: { "Content-Type": "application/json" },
    });
  }
  return null;
}
