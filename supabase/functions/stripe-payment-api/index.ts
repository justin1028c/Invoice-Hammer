import Stripe from "stripe";
import { AuthError, requireFirebaseUser } from "./lib/auth.ts";
import { corsHeaders, errorResponse, jsonResponse } from "./lib/cors.ts";
import { createServiceClient } from "./lib/db.ts";
import { createOnboardingLink, fetchConnectStatus } from "./lib/connect.ts";
import { htmlPage, paymentBridgePage } from "./lib/html.ts";
import { fetchInvoicePaymentStatus } from "./lib/invoicePayments.ts";
import { PaymentAttemptError } from "./lib/paymentAttempts.ts";
import {
  createPaymentIntent,
  PaymentError,
  resolveCheckoutLink,
  verifyCheckoutSession,
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
    "/v1/payments/verify",
    "/v1/payments/checkout-link",
    "/v1/payments/invoice-status",
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

function buildAppDeeplink(path: string, params: Record<string, string | null>): string {
  const query = Object.entries(params)
    .filter(([, value]) => value != null && value.trim().length > 0)
    .map(([key, value]) =>
      `${encodeURIComponent(key)}=${encodeURIComponent(value!.trim())}`
    )
    .join("&");
  return query.length > 0
    ? `invoicehammer://${path}?${query}`
    : `invoicehammer://${path}`;
}

function publicBrowserPage(route: string, url: URL): Response {
  if (route.endsWith("/v1/connect/return")) {
    const deeplink = "invoicehammer://stripe-callback";
    return paymentBridgePage({
      title: "Stripe setup complete",
      message: "Your Stripe Connect Express onboarding step finished. Returning to Invoice Hammer...",
      deeplink,
      buttonLabel: "Return to Invoice Hammer",
      footnote: "If the app does not open automatically, tap the button above to return.",
    });
  }
  if (route.endsWith("/v1/connect/refresh")) {
    return htmlPage(
      "Continue Stripe setup",
      "<p>Your session expired or needs more information.</p>" +
        "<p>Return to Invoice Hammer → <em>Settings → STRIPE PAYOUTS → SET UP STRIPE PAYOUTS</em> to continue.</p>",
    );
  }

  const invoiceId = url.searchParams.get("invoice_id")?.trim() ?? "";
  const sessionId = url.searchParams.get("session_id")?.trim() ?? "";
  const contractorUserId = url.searchParams.get("contractor_user_id")?.trim() ?? "";
  const lang = url.searchParams.get("lang")?.trim()?.toLowerCase();
  const isSpanish = lang === "es";

  if (route.endsWith("/v1/payments/success")) {
    const deeplink = buildAppDeeplink("payment-success", {
      invoice_id: invoiceId,
      session_id: sessionId,
      contractor_user_id: contractorUserId,
    });
    return paymentBridgePage({
      title: isSpanish ? "Pago recibido" : "Payment received",
      message: isSpanish
        ? "Gracias — Stripe registró este pago. Regresando a Invoice Hammer…"
        : "Thank you — Stripe recorded this payment. Returning to Invoice Hammer…",
      deeplink,
      buttonLabel: isSpanish ? "Abrir Invoice Hammer" : "Open Invoice Hammer",
      footnote: isSpanish
        ? "Si pagó en otro teléfono, el contratista verá el pago cuando abra la app."
        : "If you paid on another phone, the contractor will see the payment when they open the app.",
    });
  }

  const deeplink = buildAppDeeplink("payment-cancelled", {
    invoice_id: invoiceId,
    session_id: sessionId,
    contractor_user_id: contractorUserId,
  });
  return paymentBridgePage({
    title: isSpanish ? "Pago cancelado" : "Payment cancelled",
    message: isSpanish
      ? "El pago no se completó. Puede cerrar esta pestaña."
      : "Checkout was cancelled. You can close this tab.",
    deeplink,
    buttonLabel: isSpanish ? "Volver a Invoice Hammer" : "Return to Invoice Hammer",
    footnote: isSpanish
      ? "El contratista puede enviar un nuevo enlace de pago desde la app."
      : "The contractor can send a new payment link from the app.",
  });
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { status: 204, headers: corsHeaders });
  }

  const url = new URL(req.url);
  const route = routeSuffix(url.pathname);

  if (isPublicBrowserRoute(route) && req.method === "GET") {
    return publicBrowserPage(route, url);
  }

  try {
    const user = await requireFirebaseUser(req);
    const stripe = requireStripe();
    const supabase = createServiceClient();

    if (route.endsWith("/v1/payments/intent") && req.method === "POST") {
      const body = await req.json() as PaymentIntentRequest;
      body.contractorUserId = user.uid;
      const result = await createPaymentIntent(stripe, supabase, body);
      return jsonResponse(result);
    }

    if (route.endsWith("/v1/payments/verify") && req.method === "GET") {
      const sessionId = url.searchParams.get("session_id")?.trim();
      if (!sessionId) {
        return errorResponse("Query parameter session_id is required.", 400);
      }
      const result = await verifyCheckoutSession(
        stripe,
        supabase,
        sessionId,
        user.uid,
      );
      return jsonResponse(result);
    }

    if (route.endsWith("/v1/payments/checkout-link") && req.method === "GET") {
      const sessionId = url.searchParams.get("session_id")?.trim();
      if (!sessionId) {
        return errorResponse(
          "Query parameter session_id is required.",
          400,
        );
      }
      const result = await resolveCheckoutLink(
        stripe,
        supabase,
        sessionId,
        user.uid,
      );
      return jsonResponse(result);
    }

    if (route.endsWith("/v1/payments/invoice-status") && req.method === "GET") {
      const invoiceId = url.searchParams.get("invoiceId")?.trim();
      if (!invoiceId) {
        return errorResponse(
          "Query parameter invoiceId is required.",
          400,
        );
      }
      const status = await fetchInvoicePaymentStatus(
        supabase,
        invoiceId,
        user.uid,
      );
      return jsonResponse(status);
    }

    if (route.endsWith("/v1/connect/status") && req.method === "GET") {
      const status = await fetchConnectStatus(stripe, supabase, user.uid);
      const { onboardingUrl: _, ...response } = status;
      return jsonResponse(response);
    }

    if (route.endsWith("/v1/connect/onboard") && req.method === "POST") {
      const contractorUserId = user.uid;
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
    if (err instanceof AuthError) {
      return errorResponse(err.message, err.status);
    }
    if (err instanceof PaymentError) {
      return jsonResponse(
        { error: err.message, ...err.details },
        err.status,
      );
    }
    if (err instanceof PaymentAttemptError) {
      return errorResponse(err.message, err.status);
    }
    console.error("stripe-payment-api", err);
    return errorResponse("Internal payment service error.", 500);
  }
});
