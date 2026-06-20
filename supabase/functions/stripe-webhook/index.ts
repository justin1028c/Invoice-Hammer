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
  if (error) {
    console.warn("stripe_invoice_payments mark paid:", error.message);
  }
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
      await upsertPaidFromMetadata(
        supabase,
        intent.metadata,
        intent.id,
        null,
      );

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
      await upsertPaidFromMetadata(
        supabase,
        session.metadata,
        paymentIntentId,
        session.id,
      );
    }
  }

  return new Response(JSON.stringify({ received: true }), {
    status: 200,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
});
