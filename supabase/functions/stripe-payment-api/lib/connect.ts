import Stripe from "stripe";
import type { SupabaseClient } from "@supabase/supabase-js";
import { lookupStripeAccountId, saveStripeAccountId } from "./db.ts";
import { connectRefreshUrl, connectReturnUrl } from "./redirectUrls.ts";

export type ConnectStatusBody = {
  accountId: string | null;
  chargesEnabled: boolean;
  payoutsEnabled: boolean;
  onboardingUrl?: string;
};

export async function ensureExpressAccount(
  stripe: Stripe,
  supabase: SupabaseClient,
  contractorUserId: string,
): Promise<string> {
  const existing = await lookupStripeAccountId(supabase, contractorUserId);
  if (existing) return existing;

  const account = await stripe.accounts.create({
    type: "express",
    metadata: { contractor_user_id: contractorUserId },
    capabilities: {
      card_payments: { requested: true },
      transfers: { requested: true },
    },
  });

  await saveStripeAccountId(supabase, contractorUserId, account.id);
  return account.id;
}

export async function fetchConnectStatus(
  stripe: Stripe,
  supabase: SupabaseClient,
  contractorUserId: string,
): Promise<ConnectStatusBody> {
  const accountId = await lookupStripeAccountId(supabase, contractorUserId);
  if (!accountId) {
    return {
      accountId: null,
      chargesEnabled: false,
      payoutsEnabled: false,
    };
  }

  const account = await stripe.accounts.retrieve(accountId);
  const body: ConnectStatusBody = {
    accountId: account.id,
    chargesEnabled: account.charges_enabled ?? false,
    payoutsEnabled: account.payouts_enabled ?? false,
  };

  if (!body.chargesEnabled) {
    body.onboardingUrl = await createOnboardingLink(stripe, accountId);
  }

  return body;
}

export async function createOnboardingLink(
  stripe: Stripe,
  stripeAccountId: string,
): Promise<string> {
  const refresh = connectRefreshUrl();
  const returnUrl = connectReturnUrl();

  const link = await stripe.accountLinks.create({
    account: stripeAccountId,
    refresh_url: refresh,
    return_url: returnUrl,
    type: "account_onboarding",
  });
  return link.url;
}
