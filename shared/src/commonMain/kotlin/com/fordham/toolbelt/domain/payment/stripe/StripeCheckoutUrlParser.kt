package com.fordham.toolbelt.domain.payment.stripe

object StripeCheckoutUrlParser {
    fun extractSessionId(checkoutUrl: String): String? =
        Regex("""/pay/(cs_[^/?#]+)""").find(checkoutUrl)?.groupValues?.get(1)
            ?: Regex("""\b(cs_(?:test|live)_[A-Za-z0-9]+)""").find(checkoutUrl)?.value

    fun isHostedCheckoutUrl(url: String): Boolean =
        url.contains("checkout.stripe.com/c/pay/") ||
            url.contains("checkout.stripe.com/pay/")
}
