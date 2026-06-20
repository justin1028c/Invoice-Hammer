import type { SupabaseClient } from "@supabase/supabase-js";

export type InvoicePaymentRow = {
  invoice_id: string;
  contractor_user_id: string;
  checkout_session_id: string | null;
  payment_intent_id: string | null;
  status: string;
  paid_at: string | null;
};

export async function upsertPendingCheckout(
  supabase: SupabaseClient,
  invoiceId: string,
  contractorUserId: string,
  checkoutSessionId: string,
  paymentIntentId: string | null,
): Promise<void> {
  const { error } = await supabase.from("stripe_invoice_payments").upsert(
    {
      invoice_id: invoiceId,
      contractor_user_id: contractorUserId,
      checkout_session_id: checkoutSessionId,
      payment_intent_id: paymentIntentId,
      status: "pending",
      paid_at: null,
      updated_at: new Date().toISOString(),
    },
    { onConflict: "invoice_id" },
  );
  if (error) {
    console.warn("stripe_invoice_payments upsert pending:", error.message);
  }
}

export async function markInvoicePaymentPaid(
  supabase: SupabaseClient,
  invoiceId: string,
  contractorUserId: string | null,
  paymentIntentId: string | null,
  checkoutSessionId: string | null,
): Promise<void> {
  const paidAt = new Date().toISOString();
  const { data: existing } = await supabase
    .from("stripe_invoice_payments")
    .select("invoice_id, contractor_user_id")
    .eq("invoice_id", invoiceId)
    .maybeSingle();

  const row = {
    invoice_id: invoiceId,
    contractor_user_id: contractorUserId ?? existing?.contractor_user_id ?? "unknown",
    checkout_session_id: checkoutSessionId,
    payment_intent_id: paymentIntentId,
    status: "paid",
    paid_at: paidAt,
    updated_at: paidAt,
  };

  const { error } = await supabase.from("stripe_invoice_payments").upsert(
    row,
    { onConflict: "invoice_id" },
  );
  if (error) {
    console.warn("stripe_invoice_payments mark paid:", error.message);
  }
}

export async function fetchInvoicePaymentStatus(
  supabase: SupabaseClient,
  invoiceId: string,
  contractorUserId: string,
): Promise<{ paid: boolean; paidAt: string | null }> {
  const { data, error } = await supabase
    .from("stripe_invoice_payments")
    .select("status, paid_at, contractor_user_id")
    .eq("invoice_id", invoiceId)
    .maybeSingle();

  if (error) {
    throw new Error(`Invoice payment lookup failed: ${error.message}`);
  }
  if (!data || data.contractor_user_id !== contractorUserId) {
    return { paid: false, paidAt: null };
  }
  return {
    paid: data.status === "paid",
    paidAt: data.paid_at ?? null,
  };
}

export async function fetchPaymentBySessionId(
  supabase: SupabaseClient,
  sessionId: string,
): Promise<InvoicePaymentRow | null> {
  const { data, error } = await supabase
    .from("stripe_invoice_payments")
    .select("*")
    .eq("checkout_session_id", sessionId)
    .maybeSingle();

  if (error) {
    throw new Error(`Session lookup failed: ${error.message}`);
  }
  return data as InvoicePaymentRow | null;
}
