import type { SupabaseClient } from "@supabase/supabase-js";

export class PaymentAttemptError extends Error {
  constructor(message: string, readonly status = 409) {
    super(message);
  }
}

export type PaymentAttempt = {
  operation_id: string;
  contractor_user_id: string;
  invoice_id: string;
  amount_cents: number;
  currency: string;
  stripe_account_id: string;
  payment_provider: string;
  payment_intent_id: string | null;
  checkout_session_id: string | null;
  status: string;
};

export async function freezeInvoiceSnapshot(
  supabase: SupabaseClient,
  contractorUserId: string,
  invoiceId: string,
  amountCents: number,
  currency: string,
): Promise<void> {
  const { error } = await supabase.from("stripe_invoice_snapshots").insert({
    contractor_user_id: contractorUserId,
    invoice_id: invoiceId,
    amount_cents: amountCents,
    currency,
  });
  if (error && error.code !== "23505") {
    throw new Error(`Invoice snapshot failed: ${error.message}`);
  }

  const { data, error: readError } = await supabase
    .from("stripe_invoice_snapshots")
    .select("amount_cents, currency")
    .eq("contractor_user_id", contractorUserId)
    .eq("invoice_id", invoiceId)
    .single();
  if (readError) throw new Error(`Invoice snapshot lookup failed: ${readError.message}`);
  if (Number(data.amount_cents) !== amountCents || data.currency !== currency) {
    throw new PaymentAttemptError(
      "Invoice payment terms are already frozen and do not match this request.",
    );
  }
}

export async function beginPaymentAttempt(
  supabase: SupabaseClient,
  attempt: Omit<PaymentAttempt, "payment_intent_id" | "checkout_session_id" | "status">,
): Promise<PaymentAttempt> {
  const { error } = await supabase.from("stripe_payment_attempts").insert(attempt);
  if (error && error.code !== "23505") {
    throw new Error(`Payment attempt failed: ${error.message}`);
  }
  const { data, error: readError } = await supabase
    .from("stripe_payment_attempts")
    .select("*")
    .eq("operation_id", attempt.operation_id)
    .single();
  if (readError) throw new Error(`Payment attempt lookup failed: ${readError.message}`);
  const existing = data as PaymentAttempt;
  if (
    existing.contractor_user_id !== attempt.contractor_user_id ||
    existing.invoice_id !== attempt.invoice_id ||
    Number(existing.amount_cents) !== attempt.amount_cents ||
    existing.currency !== attempt.currency ||
    existing.payment_provider !== attempt.payment_provider
  ) {
    throw new PaymentAttemptError("operationId was already used for different payment terms.");
  }
  return existing;
}

export async function completePaymentAttempt(
  supabase: SupabaseClient,
  operationId: string,
  paymentIntentId: string | null,
  checkoutSessionId: string | null,
): Promise<void> {
  const { error } = await supabase.from("stripe_payment_attempts").update({
    payment_intent_id: paymentIntentId,
    checkout_session_id: checkoutSessionId,
    status: "pending",
    updated_at: new Date().toISOString(),
  }).eq("operation_id", operationId);
  if (error) throw new Error(`Payment attempt update failed: ${error.message}`);
}
