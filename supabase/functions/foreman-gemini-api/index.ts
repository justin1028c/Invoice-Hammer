import { assertForemanAuth } from "./lib/auth.ts";
import { corsHeaders, errorResponse } from "./lib/cors.ts";
import { checkRateLimit } from "./lib/rateLimit.ts";

interface ProxyBody {
  model?: string;
  request?: unknown;
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { status: 204, headers: corsHeaders });
  }

  const url = new URL(req.url);
  if (!url.pathname.endsWith("/v1/generate")) {
    return errorResponse("Not found", 404);
  }

  if (req.method !== "POST") {
    return errorResponse("Method not allowed", 405);
  }

  const authFailure = assertForemanAuth(req);
  if (authFailure) return authFailure;

  const rateKey = req.headers.get("x-foreman-backend-key")?.trim() ?? "anonymous";
  const rateFailure = checkRateLimit(rateKey);
  if (rateFailure) {
    return new Response(rateFailure.body, {
      status: rateFailure.status,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }

  let body: ProxyBody;
  try {
    body = await req.json() as ProxyBody;
  } catch {
    return errorResponse("Invalid JSON body", 400);
  }

  const model = body.model?.trim() ||
    Deno.env.get("GEMINI_MODEL")?.trim();
  if (!model) {
    return errorResponse("model is required (body.model or GEMINI_MODEL secret)", 400);
  }
  if (body.request == null) {
    return errorResponse("request is required", 400);
  }

  const apiKey = Deno.env.get("GEMINI_API_KEY")?.trim();
  if (!apiKey) {
    return errorResponse("GEMINI_API_KEY is not configured on the server", 500);
  }

  const googleUrl =
    `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(model)}:generateContent`;

  const googleRes = await fetch(googleUrl, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "x-goog-api-key": apiKey,
    },
    body: JSON.stringify(body.request),
  });

  const responseText = await googleRes.text();
  return new Response(responseText, {
    status: googleRes.status,
    headers: {
      ...corsHeaders,
      "Content-Type": googleRes.headers.get("Content-Type") ?? "application/json",
    },
  });
});
