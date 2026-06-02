import Stripe from "stripe";
import { assertBackendAuth } from "./lib/auth.ts";
import { corsHeaders, errorResponse, jsonResponse } from "./lib/cors.ts";
import { createServiceClient } from "./lib/db.ts";
import { createOnboardingLink, fetchConnectStatus } from "./lib/connect.ts";
import { htmlPage } from "./lib/html.ts";
import {
  createPaymentIntent,
  PaymentError,
  type PaymentIntentRequest,
} from "./lib/payments.ts";

function requireStripe(): Stripe {
  const key = Deno.env.get("STRIPE_SECRET_KEY")?.trim();
  if (!key) {
    throw new Error("STRIPE_SECRET_KEY is not configured.");
  }
  return new Stripe(key, {
    httpClient: Stripe.createFetchHttpClient(),
  });
}

function routeSuffix(pathname: string): string {
  const normalized = pathname.replace(/\/+$/, "");
  const markers = [
    "/v1/payments/intent",
    "/v1/payments/success",
    "/v1/payments/cancel",
    "/v1/connect/status",
    "/v1/connect/onboard",
    "/v1/connect/return",
    "/v1/connect/refresh",
  ];
  for (const marker of markers) {
    if (normalized.endsWith(marker)) return marker;
  }
  return normalized;
}

function isPublicBrowserRoute(route: string): boolean {
  return route.endsWith("/v1/connect/return") ||
    route.endsWith("/v1/connect/refresh") ||
    route.endsWith("/v1/payments/success") ||
    route.endsWith("/v1/payments/cancel");
}

function publicBrowserPage(route: string): Response {
  if (route.endsWith("/v1/connect/return")) {
    return htmlPage(
      "Stripe setup complete",
      "<p>Your Stripe Connect onboarding step finished.</p>" +
        "<p><strong>Switch back to Invoice Hammer</strong>, open " +
        "<em>Settings → STRIPE PAYOUTS</em>, and tap <strong>Refresh status</strong>.</p>",
    );
  }
  if (route.endsWith("/v1/connect/refresh")) {
    return htmlPage(
      "Continue Stripe setup",
      "<p>Your session expired or needs more information.</p>" +
        "<p>Return to Invoice Hammer → <em>Settings → STRIPE PAYOUTS → SET UP STRIPE PAYOUTS</em> to continue.</p>",
    );
  }
  if (route.endsWith("/v1/payments/success")) {
    return htmlPage(
      "Payment received",
      "<p>Thank you — Stripe recorded this payment.</p><p>You can close this tab.</p>",
    );
  }
  return htmlPage(
    "Payment cancelled",
    "<p>Checkout was cancelled. You can close this tab.</p>",
  );
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { status: 204, headers: corsHeaders });
  }

  const url = new URL(req.url);
  const route = routeSuffix(url.pathname);

  if (isPublicBrowserRoute(route) && req.method === "GET") {
    return publicBrowserPage(route);
  }

  const authFailure = assertBackendAuth(req);
  if (authFailure) {
    return new Response(authFailure.body, {
      status: authFailure.status,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }

  try {
    const stripe = requireStripe();
    const supabase = createServiceClient();

    if (route.endsWith("/v1/payments/intent") && req.method === "POST") {
      const body = await req.json() as PaymentIntentRequest;
      const result = await createPaymentIntent(stripe, supabase, body);
      return jsonResponse(result);
    }

    if (route.endsWith("/v1/connect/status") && req.method === "GET") {
      const userId = url.searchParams.get("userId")?.trim();
      if (!userId) {
        return errorResponse("Query parameter userId is required.", 400);
      }
      const status = await fetchConnectStatus(stripe, supabase, userId);
      const { onboardingUrl: _, ...response } = status;
      return jsonResponse(response);
    }

    if (route.endsWith("/v1/connect/onboard") && req.method === "POST") {
      const body = await req.json() as { contractorUserId?: string };
      const contractorUserId = body.contractorUserId?.trim();
      if (!contractorUserId) {
        return errorResponse("contractorUserId is required.", 400);
      }
      const status = await fetchConnectStatus(stripe, supabase, contractorUserId);
      if (!status.accountId) {
        return errorResponse("Could not create Connect account.", 500);
      }
      const onboardingUrl = status.onboardingUrl ??
        await createOnboardingLink(stripe, status.accountId);
      return jsonResponse({ onboardingUrl, accountId: status.accountId });
    }

    return errorResponse("Not found", 404);
  } catch (err) {
    if (err instanceof PaymentError) {
      return jsonResponse(
        { error: err.message, ...err.details },
        err.status,
      );
    }
    console.error("stripe-payment-api", err);
    const message = err instanceof Error ? err.message : "Internal error";
    return errorResponse(message, 500);
  }
});
