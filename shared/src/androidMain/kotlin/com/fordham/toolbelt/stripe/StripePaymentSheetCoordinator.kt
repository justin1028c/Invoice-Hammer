package com.fordham.toolbelt.stripe

import androidx.fragment.app.FragmentActivity
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

object StripePaymentSheetCoordinator {
    private var paymentSheet: PaymentSheet? = null
    private var pendingContinuation: Continuation<PaymentSheetResult>? = null

    fun register(activity: FragmentActivity, publishableKey: String) {
        if (!publishableKey.startsWith("pk_")) return
        PaymentConfiguration.init(activity.applicationContext, publishableKey)
        paymentSheet = PaymentSheet(activity) { result ->
            pendingContinuation?.resume(result)
            pendingContinuation = null
        }
    }

    fun present(
        clientSecret: String,
        stripeAccountId: String,
        continuation: Continuation<PaymentSheetResult>
    ) {
        val sheet = paymentSheet
        val activity = com.fordham.toolbelt.billing.BillingActivityHolder.activity
        if (sheet == null || activity == null) {
            continuation.resume(
                PaymentSheetResult.Failed(
                    IllegalStateException("Stripe PaymentSheet not initialized. Restart the app.")
                )
            )
            return
        }
        val publishableKey = PaymentConfiguration.getInstance(activity.applicationContext).publishableKey
        if (stripeAccountId.isNotBlank()) {
            PaymentConfiguration.init(
                activity.applicationContext,
                publishableKey,
                stripeAccountId
            )
        }
        pendingContinuation = continuation
        sheet.presentWithPaymentIntent(clientSecret)
    }
}
