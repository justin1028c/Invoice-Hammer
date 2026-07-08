import Stripe from "stripe";
import { createClient } from "@supabase/supabase-js";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers":
    "authorization, content-type, stripe-signature",
};

async function markInvoicePaymentPaid(
  supabase: ReturnType<typeof createClient>,
  invoiceId: string,
  contractorUserId: string,
  paymentIntentId: string | null,
  checkoutSessionId: string | null,
): Promise<void> {
  const paidAt = new Date().toISOString();
  const { error } = await supabase.from("stripe_invoice_payments").upsert(
    {
      invoice_id: invoiceId,
      contractor_user_id: contractorUserId,
      checkout_session_id: checkoutSessionId,
      payment_intent_id: paymentIntentId,
      status: "paid",
      paid_at: paidAt,
      updated_at: paidAt,
    },
    { onConflict: "invoice_id" },
  );
  if (error) throw new Error(`stripe_invoice_payments mark paid: ${error.message}`);
}

async function upsertPaidFromMetadata(
  supabase: ReturnType<typeof createClient>,
  metadata: Stripe.Metadata | null | undefined,
  paymentIntentId: string | null,
  checkoutSessionId: string | null,
): Promise<void> {
  const invoiceId = metadata?.invoice_id?.trim();
  const contractorUserId = metadata?.contractor_user_id?.trim();
  if (!invoiceId || !contractorUserId) return;

  await markInvoicePaymentPaid(
    supabase,
    invoiceId,
    contractorUserId,
    paymentIntentId,
    checkoutSessionId,
  );
}

async function validateAttempt(
  supabase: ReturnType<typeof createClient>,
  event: Stripe.Event,
  metadata: Stripe.Metadata | null | undefined,
  paymentIntentId: string | null,
  checkoutSessionId: string | null,
  amountCents: number | null,
  currency: string | null,
): Promise<boolean> {
  const operationId = metadata?.operation_id?.trim();
  if (!operationId) return false;
  const { data, error } = await supabase
    .from("stripe_payment_attempts")
    .select("*")
    .eq("operation_id", operationId)
    .single();
  if (error || !data) throw new Error(`Payment attempt lookup failed: ${error?.message ?? "missing"}`);

  const valid =
    data.invoice_id === metadata?.invoice_id &&
    data.contractor_user_id === metadata?.contractor_user_id &&
    (amountCents == null || Number(data.amount_cents) === amountCents) &&
    (currency == null || data.currency === currency.toLowerCase()) &&
    (!data.payment_intent_id || data.payment_intent_id === paymentIntentId) &&
    (!data.checkout_session_id || data.checkout_session_id === checkoutSessionId) &&
    (!event.account || data.stripe_account_id === event.account);
  if (!valid) {
    console.error(JSON.stringify({ event: "stripe_attempt_validation_failed", operationId, stripeEventId: event.id }));
    return false;
  }

  const { error: updateError } = await supabase
    .from("stripe_payment_attempts")
    .update({ status: "paid", updated_at: new Date().toISOString() })
    .eq("operation_id", operationId);
  if (updateError) throw new Error(`Payment attempt update failed: ${updateError.message}`);
  return true;
}

async function transitionByPaymentIntent(
  supabase: ReturnType<typeof createClient>,
  paymentIntentId: string,
  status: string,
  failureCode: string | null = null,
): Promise<void> {
  const { data, error } = await supabase
    .from("stripe_payment_attempts")
    .select("operation_id, invoice_id")
    .eq("payment_intent_id", paymentIntentId)
    .maybeSingle();
  if (error) throw new Error(`Payment attempt transition lookup failed: ${error.message}`);
  if (!data) return;

  const now = new Date().toISOString();
  const { error: attemptError } = await supabase
    .from("stripe_payment_attempts")
    .update({ status, updated_at: now })
    .eq("operation_id", data.operation_id);
  if (attemptError) throw new Error(`Payment attempt transition failed: ${attemptError.message}`);

  const invoicePatch: Record<string, string | null> = {
    status,
    failure_code: failureCode,
    updated_at: now,
  };
  if (status === "refunded") invoicePatch.refunded_at = now;
  if (status === "disputed") invoicePatch.disputed_at = now;
  const { error: invoiceError } = await supabase
    .from("stripe_invoice_payments")
    .update(invoicePatch)
    .eq("invoice_id", data.invoice_id);
  if (invoiceError) throw new Error(`Invoice payment transition failed: ${invoiceError.message}`);
}

async function transitionBySession(
  supabase: ReturnType<typeof createClient>,
  checkoutSessionId: string,
  status: string,
): Promise<void> {
  const now = new Date().toISOString();
  const { data, error } = await supabase
    .from("stripe_payment_attempts")
    .update({ status, updated_at: now })
    .eq("checkout_session_id", checkoutSessionId)
    .select("invoice_id")
    .maybeSingle();
  if (error) throw new Error(`Checkout transition failed: ${error.message}`);
  if (!data) return;
  const { error: invoiceError } = await supabase
    .from("stripe_invoice_payments")
    .update({ status, updated_at: now })
    .eq("invoice_id", data.invoice_id)
    .neq("status", "paid");
  if (invoiceError) throw new Error(`Invoice checkout transition failed: ${invoiceError.message}`);
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { status: 204, headers: corsHeaders });
  }
  if (req.method !== "POST") {
    return new Response("Method not allowed", { status: 405, headers: corsHeaders });
  }

  const stripeKey = Deno.env.get("STRIPE_SECRET_KEY")?.trim();
  const webhookSecret = Deno.env.get("STRIPE_WEBHOOK_SECRET")?.trim();
  if (!stripeKey || !webhookSecret) {
    return new Response("Stripe webhook not configured", { status: 500 });
  }

  const stripe = new Stripe(stripeKey, {
    httpClient: Stripe.createFetchHttpClient(),
  });
  const signature = req.headers.get("stripe-signature");
  if (!signature) {
    return new Response("Missing stripe-signature", { status: 400 });
  }

  const body = await req.text();
  let event: Stripe.Event;
  try {
    event = await stripe.webhooks.constructEventAsync(
      body,
      signature,
      webhookSecret,
    );
  } catch (err) {
    const message = err instanceof Error ? err.message : "Invalid signature";
    console.error("stripe-webhook verify failed", message);
    return new Response(message, { status: 400 });
  }

  const url = Deno.env.get("SUPABASE_URL");
  const key = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  const supabase = url && key
    ? createClient(url, key, { auth: { persistSession: false } })
    : null;

  if (event.type === "payment_intent.succeeded") {
    const intent = event.data.object as Stripe.PaymentIntent;
    const invoiceId = intent.metadata?.invoice_id;
    const contractorUserId = intent.metadata?.contractor_user_id;
    console.info(
      JSON.stringify({
        event: event.type,
        paymentIntentId: intent.id,
        invoiceId,
        contractorUserId,
        amount: intent.amount_received,
      }),
    );

    if (supabase) {
      const valid = await validateAttempt(
        supabase, event, intent.metadata, intent.id, null,
        intent.amount_received, intent.currency,
      );
      if (!valid) return new Response("Payment validation failed", { status: 400 });
      await upsertPaidFromMetadata(supabase, intent.metadata, intent.id, null);

      if (invoiceId) {
        await supabase.from("stripe_payment_events").insert({
          stripe_event_id: event.id,
          payment_intent_id: intent.id,
          invoice_id: invoiceId,
          contractor_user_id: contractorUserId ?? null,
          amount_cents: intent.amount_received,
          currency: intent.currency,
          status: "succeeded",
          raw_metadata: intent.metadata,
        }).then(({ error }) => {
          if (error) {
            console.warn("stripe_payment_events insert skipped:", error.message);
          }
        });
      }
    }
  }

  if (event.type === "checkout.session.completed") {
    const session = event.data.object as Stripe.Checkout.Session;
    const paymentIntentId = typeof session.payment_intent === "string"
      ? session.payment_intent
      : session.payment_intent?.id ?? null;

    console.info(
      JSON.stringify({
        event: event.type,
        sessionId: session.id,
        invoiceId: session.metadata?.invoice_id,
        paymentStatus: session.payment_status,
      }),
    );

    if (supabase && session.payment_status === "paid") {
      const valid = await validateAttempt(
        supabase, event, session.metadata, paymentIntentId, session.id,
        session.amount_total, session.currency,
      );
      if (!valid) return new Response("Payment validation failed", { status: 400 });
      await upsertPaidFromMetadata(supabase, session.metadata, paymentIntentId, session.id);
    }
  }

  if (supabase && event.type === "payment_intent.payment_failed") {
    const intent = event.data.object as Stripe.PaymentIntent;
    await transitionByPaymentIntent(
      supabase,
      intent.id,
      "failed",
      intent.last_payment_error?.code ?? null,
    );
  }

  if (supabase && event.type === "payment_intent.canceled") {
    const intent = event.data.object as Stripe.PaymentIntent;
    await transitionByPaymentIntent(supabase, intent.id, "cancelled");
  }

  if (supabase && event.type === "checkout.session.expired") {
    const session = event.data.object as Stripe.Checkout.Session;
    await transitionBySession(supabase, session.id, "expired");
  }

  if (supabase && event.type === "charge.refunded") {
    const charge = event.data.object as Stripe.Charge;
    const paymentIntentId = typeof charge.payment_intent === "string"
      ? charge.payment_intent
      : charge.payment_intent?.id;
    if (paymentIntentId) {
      await transitionByPaymentIntent(supabase, paymentIntentId, "refunded");
    }
  }

  if (supabase && event.type === "charge.dispute.created") {
    const dispute = event.data.object as Stripe.Dispute;
    const chargeId = typeof dispute.charge === "string"
      ? dispute.charge
      : dispute.charge.id;
    const charge = event.account
      ? await stripe.charges.retrieve(chargeId, {}, { stripeAccount: event.account })
      : await stripe.charges.retrieve(chargeId);
    const paymentIntentId = typeof charge.payment_intent === "string"
      ? charge.payment_intent
      : charge.payment_intent?.id;
    if (paymentIntentId) {
      await transitionByPaymentIntent(supabase, paymentIntentId, "disputed");
    }
  }

  return new Response(JSON.stringify({ received: true }), {
    status: 200,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
});
