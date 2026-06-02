import Stripe from "stripe";
import type { SupabaseClient } from "@supabase/supabase-js";
import { createOnboardingLink, ensureExpressAccount } from "./connect.ts";
import { checkoutCancelUrl, checkoutSuccessUrl } from "./redirectUrls.ts";

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
    `?invoice_id=${encodeURIComponent(req.invoiceId)}`;
  const cancelUrl = checkoutCancelUrl() +
    `?invoice_id=${encodeURIComponent(req.invoiceId)}`;

  const session = await stripe.checkout.sessions.create(
    {
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
        metadata,
      },
      metadata,
      payment_method_types: ["card", "link"],
    },
    connectOpts,
  );

  if (!session.url) {
    throw new PaymentError("Stripe Checkout session has no URL.", 502);
  }

  // PaymentIntent may not expose client_secret until the session is expanded.
  const expanded = await stripe.checkout.sessions.retrieve(
    session.id,
    { expand: ["payment_intent"] },
    connectOpts,
  );

  const piField = expanded.payment_intent;
  const paymentIntentId = typeof piField === "string"
    ? piField
    : piField?.id ?? "";

  let clientSecret = "";
  if (piField && typeof piField !== "string") {
    clientSecret = piField.client_secret ?? "";
  } else if (paymentIntentId) {
    const intent = await stripe.paymentIntents.retrieve(
      paymentIntentId,
      connectOpts,
    );
    clientSecret = intent.client_secret ?? "";
  }

  // Hosted Checkout: client pays via session.url — clientSecret is optional.
  return {
    clientSecret,
    paymentIntentId: paymentIntentId || expanded.id,
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
