import { assertForemanAuth } from "./lib/auth.ts";
import { corsHeaders, errorResponse } from "./lib/cors.ts";
import { checkRateLimit } from "./lib/rateLimit.ts";

interface ProxyBody {
  model?: string;
  request?: unknown;
}

Deno.serve(async (req) => {
  console.log(`[Request] Method: ${req.method}, URL: ${req.url}`);
  if (req.method === "OPTIONS") {
    return new Response(null, { status: 204, headers: corsHeaders });
  }

  const url = new URL(req.url);
  const apiKey = Deno.env.get("GEMINI_API_KEY")?.trim();

  if (!url.pathname.endsWith("/v1/generate")) {
    console.warn(`[Warning] Path not matched: ${url.pathname}`);
    return errorResponse("Not found", 404);
  }

  if (req.method !== "POST") {
    console.warn(`[Warning] Method not allowed: ${req.method}`);
    return errorResponse("Method not allowed", 405);
  }

  const authFailure = assertForemanAuth(req);
  if (authFailure) {
    console.warn("[Warning] Auth verification failed");
    return authFailure;
  }

  const rateKey = req.headers.get("x-foreman-backend-key")?.trim() ?? "anonymous";
  const rateFailure = checkRateLimit(rateKey);
  if (rateFailure) {
    console.warn(`[Warning] Rate limit exceeded for key: ${rateKey}`);
    return new Response(rateFailure.body, {
      status: rateFailure.status,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }

  let body: ProxyBody;
  try {
    body = await req.json() as ProxyBody;
  } catch (err) {
    console.error("[Error] Failed to parse request JSON:", err);
    return errorResponse("Invalid JSON body", 400);
  }

  const model = body.model?.trim() || Deno.env.get("GEMINI_MODEL")?.trim();
  if (!model) {
    console.error("[Error] Model name not specified");
    return errorResponse("model is required (body.model or GEMINI_MODEL secret)", 400);
  }
  if (body.request == null) {
    console.error("[Error] Request payload missing");
    return errorResponse("request is required", 400);
  }

  if (!apiKey) {
    console.error("[Error] GEMINI_API_KEY secret not found on server");
    return errorResponse("GEMINI_API_KEY is not configured on the server", 500);
  }

  const googleUrl =
    `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(model)}:generateContent`;

  console.log(`[Google API] Model: ${model}, URL: ${googleUrl}`);

  const controller = new AbortController();
  const timeoutId = setTimeout(() => {
    console.error(`[Error] Google API fetch timed out after 10s for model ${model}`);
    controller.abort();
  }, 10000);

  try {
    const googleRes = await fetch(googleUrl, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "x-goog-api-key": apiKey,
      },
      body: JSON.stringify(body.request),
      signal: controller.signal,
    });
    clearTimeout(timeoutId);

    console.log(`[Google API] Response status: ${googleRes.status}`);
    const responseText = await googleRes.text();
    return new Response(responseText, {
      status: googleRes.status,
      headers: {
        ...corsHeaders,
        "Content-Type": googleRes.headers.get("Content-Type") ?? "application/json",
      },
    });
  } catch (err) {
    clearTimeout(timeoutId);
    console.error("[Error] Fetch to Google API failed:", err);
    return errorResponse(`Google API connection failed: ${err.message || err}`, 502);
  }
});
