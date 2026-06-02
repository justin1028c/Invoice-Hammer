const LIMIT_PER_MINUTE = 60;
const WINDOW_MS = 60_000;

const buckets = new Map<string, { count: number; resetAt: number }>();

export function checkRateLimit(clientKey: string): Response | null {
  const now = Date.now();
  const bucket = buckets.get(clientKey) ?? { count: 0, resetAt: now + WINDOW_MS };
  if (now > bucket.resetAt) {
    bucket.count = 0;
    bucket.resetAt = now + WINDOW_MS;
  }
  bucket.count += 1;
  buckets.set(clientKey, bucket);
  if (bucket.count > LIMIT_PER_MINUTE) {
    return new Response(JSON.stringify({ error: "Rate limit exceeded. Try again shortly." }), {
      status: 429,
      headers: { "Content-Type": "application/json" },
    });
  }
  return null;
}
