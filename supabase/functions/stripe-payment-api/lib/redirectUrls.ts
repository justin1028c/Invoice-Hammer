/** Hosted return URLs — must be HTTPS and reachable (not placeholder domains). */
export function stripeApiPublicBase(): string {
  const supabaseUrl = Deno.env.get("SUPABASE_URL")?.replace(/\/+$/, "") ??
    "https://ygvqmexpvdsdnxzmlfml.supabase.co";
  return `${supabaseUrl}/functions/v1/stripe-payment-api`;
}

export function connectReturnUrl(): string {
  return Deno.env.get("STRIPE_CONNECT_RETURN_URL")?.trim() ??
    `${stripeApiPublicBase()}/v1/connect/return`;
}

export function connectRefreshUrl(): string {
  return Deno.env.get("STRIPE_CONNECT_REFRESH_URL")?.trim() ??
    `${stripeApiPublicBase()}/v1/connect/refresh`;
}

export function checkoutSuccessUrl(): string {
  return Deno.env.get("STRIPE_CHECKOUT_SUCCESS_URL")?.trim() ??
    `${stripeApiPublicBase()}/v1/payments/success`;
}

export function checkoutCancelUrl(): string {
  return Deno.env.get("STRIPE_CHECKOUT_CANCEL_URL")?.trim() ??
    `${stripeApiPublicBase()}/v1/payments/cancel`;
}
