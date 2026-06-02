import { createClient, SupabaseClient } from "@supabase/supabase-js";

export function createServiceClient(): SupabaseClient {
  const url = Deno.env.get("SUPABASE_URL");
  const key = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!url || !key) {
    throw new Error(
      "SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY must be set for stripe-payment-api.",
    );
  }
  return createClient(url, key, { auth: { persistSession: false } });
}

export async function lookupStripeAccountId(
  supabase: SupabaseClient,
  contractorUserId: string,
): Promise<string | null> {
  const { data, error } = await supabase
    .from("stripe_connect_accounts")
    .select("stripe_account_id")
    .eq("contractor_user_id", contractorUserId)
    .maybeSingle();

  if (error) throw new Error(`Database lookup failed: ${error.message}`);
  return data?.stripe_account_id ?? null;
}

export async function saveStripeAccountId(
  supabase: SupabaseClient,
  contractorUserId: string,
  stripeAccountId: string,
): Promise<void> {
  const { error } = await supabase.from("stripe_connect_accounts").upsert(
    {
      contractor_user_id: contractorUserId,
      stripe_account_id: stripeAccountId,
      updated_at: new Date().toISOString(),
    },
    { onConflict: "contractor_user_id" },
  );
  if (error) throw new Error(`Database save failed: ${error.message}`);
}
