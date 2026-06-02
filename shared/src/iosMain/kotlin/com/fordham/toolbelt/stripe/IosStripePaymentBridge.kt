package com.fordham.toolbelt.stripe

data class IosStripePaymentNativeResult(
    val success: Boolean,
    val cancelled: Boolean = false,
    val errorMessage: String? = null
)

interface IosStripePaymentBridge {
    suspend fun presentPaymentSheet(clientSecret: String, stripeAccountId: String): IosStripePaymentNativeResult
}

object IosStripePaymentBridgeProvider {
    var bridge: IosStripePaymentBridge? = null
}
