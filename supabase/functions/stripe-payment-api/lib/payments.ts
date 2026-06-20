import Stripe from "stripe";
import type { SupabaseClient } from "@supabase/supabase-js";
import { createOnboardingLink, ensureExpressAccount } from "./connect.ts";
import { checkoutCancelUrl, checkoutSuccessUrl } from "./redirectUrls.ts";
import {
  fetchPaymentBySessionId,
  markInvoicePaymentPaid,
  upsertPendingCheckout,
} from "./invoicePayments.ts";
import { lookupStripeAccountId } from "./db.ts";

export type PaymentIntentRequest = {
  amountCents: number;
  currency: string;
  invoiceId: string;
  contractorUserId: string;
  clientName: string;
  requestType: string;
  paymentProvider: string;
  applicationFeeBps: number;
};

export type PaymentIntentResponse = {
  clientSecret: string;
  paymentIntentId: string;
  checkoutSessionId?: string | null;
  stripeAccountId: string;
  hostedCheckoutUrl?: string | null;
};

const ON_SITE_PROVIDERS = new Set([
  "card_terminal",
  "tap_to_pay",
  "bluetooth_reader",
]);

const REMOTE_PROVIDERS = new Set([
  "google_pay",
  "apple_pay",
  "card_link",
]);

export function applicationFeeAmount(
  amountCents: number,
  applicationFeeBps: number,
): number {
  if (applicationFeeBps <= 0) return 0;
  const fee = Math.round((amountCents * applicationFeeBps) / 10_000);
  return Math.min(Math.max(fee, 0), amountCents - 1);
}

function isMissingCheckoutSession(error: unknown): boolean {
  if (!(error instanceof Error)) return false;
  const stripeError = error as { code?: string; message?: string };
  return stripeError.code === "resource_missing" ||
    stripeError.message?.includes("No such checkout.session") === true;
}

async function retrieveCheckoutSession(
  stripe: Stripe,
  sessionId: string,
  connectedAccountId: string | null,
): Promise<Stripe.Checkout.Session> {
  try {
    return await stripe.checkout.sessions.retrieve(sessionId);
  } catch (error) {
    if (connectedAccountId && isMissingCheckoutSession(error)) {
      return await stripe.checkout.sessions.retrieve(
        sessionId,
        {},
        { stripeAccount: connectedAccountId },
      );
    }
    throw error;
  }
}

function productLabel(req: PaymentIntentRequest): string {
  const kind = req.requestType === "deposit" ? "Deposit" : "Payment";
  const client = req.clientName?.trim() || "Client";
  return `Invoice ${req.invoiceId} — ${kind} (${client})`;
}

export async function createPaymentIntent(
  stripe: Stripe,
  supabase: SupabaseClient,
  req: PaymentIntentRequest,
): Promise<PaymentIntentResponse> {
  if (req.amountCents < 50) {
    throw new PaymentError("amountCents must be at least 50.", 400);
  }
  if (!req.contractorUserId?.trim()) {
    throw new PaymentError("contractorUserId is required.", 400);
  }
  if (!req.invoiceId?.trim()) {
    throw new PaymentError("invoiceId is required.", 400);
  }

  const provider = req.paymentProvider?.trim() || "card_link";
  if (!ON_SITE_PROVIDERS.has(provider) && !REMOTE_PROVIDERS.has(provider)) {
    throw new PaymentError(`Unknown paymentProvider: ${provider}`, 400);
  }

  const stripeAccountId = await ensureExpressAccount(
    stripe,
    supabase,
    req.contractorUserId.trim(),
  );

  const account = await stripe.accounts.retrieve(stripeAccountId);
  if (!account.charges_enabled) {
    const onboardingUrl = await createOnboardingLink(stripe, stripeAccountId);
    throw new PaymentError(
      "Contractor must finish Stripe Connect onboarding before accepting payments.",
      409,
      { onboardingUrl },
    );
  }

  const currency = (req.currency || "usd").toLowerCase();
  const fee = applicationFeeAmount(req.amountCents, req.applicationFeeBps);
  const connectOpts: Stripe.RequestOptions = { stripeAccount: stripeAccountId };
  const metadata = {
    invoice_id: req.invoiceId,
    contractor_user_id: req.contractorUserId,
    request_type: req.requestType,
    payment_provider: provider,
  };

  if (ON_SITE_PROVIDERS.has(provider)) {
    const intent = await stripe.paymentIntents.create(
      {
        amount: req.amountCents,
        currency,
        application_fee_amount: fee > 0 ? fee : undefined,
        automatic_payment_methods: { enabled: true },
        metadata,
        description: productLabel(req),
      },
      connectOpts,
    );

    if (!intent.client_secret) {
      throw new PaymentError("Stripe did not return a client secret.", 502);
    }

    return {
      clientSecret: intent.client_secret,
      paymentIntentId: intent.id,
      stripeAccountId,
      hostedCheckoutUrl: null,
    };
  }

  const successUrl = checkoutSuccessUrl() +
    `?invoice_id=${encodeURIComponent(req.invoiceId)}` +
    `&contractor_user_id=${encodeURIComponent(req.contractorUserId)}` +
    `&session_id={CHECKOUT_SESSION_ID}`;
  const cancelUrl = checkoutCancelUrl() +
    `?invoice_id=${encodeURIComponent(req.invoiceId)}` +
    `&contractor_user_id=${encodeURIComponent(req.contractorUserId)}`;

  const session = await stripe.checkout.sessions.create({
    mode: "payment",
    success_url: successUrl,
    cancel_url: cancelUrl,
    line_items: [
      {
        quantity: 1,
        price_data: {
          currency,
          unit_amount: req.amountCents,
          product_data: { name: productLabel(req) },
        },
      },
    ],
    payment_intent_data: {
      application_fee_amount: fee > 0 ? fee : undefined,
      transfer_data: {
        destination: stripeAccountId,
      },
      metadata,
    },
    metadata,
    payment_method_types: ["card", "link"],
  });

  if (!session.url) {
    throw new PaymentError("Stripe Checkout session has no URL.", 502);
  }

  await upsertPendingCheckout(
    supabase,
    req.invoiceId.trim(),
    req.contractorUserId.trim(),
    session.id,
    typeof session.payment_intent === "string"
      ? session.payment_intent
      : session.payment_intent?.id ?? null,
  );

  const expanded = await stripe.checkout.sessions.retrieve(session.id, {
    expand: ["payment_intent"],
  });

  const piField = expanded.payment_intent;
  const paymentIntentId = typeof piField === "string"
    ? piField
    : piField?.id ?? "";

  let clientSecret = "";
  if (piField && typeof piField !== "string") {
    clientSecret = piField.client_secret ?? "";
  } else if (paymentIntentId) {
    const intent = await stripe.paymentIntents.retrieve(paymentIntentId);
    clientSecret = intent.client_secret ?? "";
  }

  // Hosted Checkout: client pays via session.url — clientSecret is optional.
  return {
    clientSecret,
    paymentIntentId: paymentIntentId,
    checkoutSessionId: session.id,
    stripeAccountId,
    hostedCheckoutUrl: session.url,
  };
}

export class PaymentError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly details?: Record<string, unknown>,
  ) {
    super(message);
    this.name = "PaymentError";
  }
}

export async function verifyCheckoutSession(
  stripe: Stripe,
  supabase: SupabaseClient,
  sessionId: string,
  contractorUserIdHint?: string | null,
): Promise<{ paid: boolean; invoiceId: string | null }> {
  const trimmed = sessionId?.trim();
  if (!trimmed) {
    throw new PaymentError("session_id is required.", 400);
  }

  const cached = await fetchPaymentBySessionId(supabase, trimmed);
  if (cached?.status === "paid") {
    return { paid: true, invoiceId: cached.invoice_id };
  }

  const contractorUserId = cached?.contractor_user_id ??
    contractorUserIdHint?.trim() ??
    null;
  const stripeAccountId = contractorUserId
    ? await lookupStripeAccountId(supabase, contractorUserId)
    : null;

  const session = await retrieveCheckoutSession(stripe, trimmed, stripeAccountId);
  const invoiceId = session.metadata?.invoice_id ??
    cached?.invoice_id ??
    null;
  const contractorId = session.metadata?.contractor_user_id ??
    contractorUserId ??
    null;
  const paid = session.payment_status === "paid";

  if (paid && invoiceId && contractorId) {
    const paymentIntentId = typeof session.payment_intent === "string"
      ? session.payment_intent
      : session.payment_intent?.id ?? null;
    await markInvoicePaymentPaid(
      supabase,
      invoiceId,
      contractorId,
      paymentIntentId,
      session.id,
    );
  }

  return { paid, invoiceId };
}

export async function resolveCheckoutLink(
  stripe: Stripe,
  supabase: SupabaseClient,
  sessionId: string,
  contractorUserId: string,
): Promise<{
  checkoutUrl: string | null;
  status: string;
  paid: boolean;
  canPay: boolean;
}> {
  const trimmed = sessionId?.trim();
  if (!trimmed) {
    throw new PaymentError("session_id is required.", 400);
  }
  const contractorId = contractorUserId?.trim();
  if (!contractorId) {
    throw new PaymentError("contractorUserId is required.", 400);
  }

  const stripeAccountId = await lookupStripeAccountId(supabase, contractorId);
  if (!stripeAccountId) {
    throw new PaymentError("Contractor Connect account not found.", 404);
  }

  const session = await retrieveCheckoutSession(stripe, trimmed, stripeAccountId);
  const paid = session.payment_status === "paid";
  const status = session.status ?? "unknown";
  const canPay = status === "open" && !paid;

  return {
    checkoutUrl: canPay ? session.url ?? null : null,
    status,
    paid,
    canPay,
  };
}
