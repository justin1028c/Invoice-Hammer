package com.fordham.toolbelt.domain.repository

import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.model.subscription.ProductId
import com.fordham.toolbelt.domain.model.subscription.PurchaseToken
import com.fordham.toolbelt.domain.model.subscription.SubscriptionTier

sealed interface StoreProductQueryOutcome {
    data class Success(val storeProductIds: List<String>) : StoreProductQueryOutcome
    data class Failure(val error: FailureMessage) : StoreProductQueryOutcome
}

data class StorePurchaseResult(
    val productId: String,
    val purchaseToken: String?,
    val isAcknowledged: Boolean
)

sealed interface StorePurchaseOutcome {
    data class Success(val result: StorePurchaseResult) : StorePurchaseOutcome
    data object Cancelled : StorePurchaseOutcome
    data class Failure(val error: FailureMessage) : StorePurchaseOutcome
}

sealed interface StoreRestoreOutcome {
    data class Success(val activeProductIds: List<String>) : StoreRestoreOutcome
    data class Failure(val error: FailureMessage) : StoreRestoreOutcome
}

/**
 * Platform store billing (Google Play Billing / App Store StoreKit).
 * Implementations live in androidMain and iosMain.
 */
interface StoreBillingGateway {
    suspend fun queryProductIds(tiers: List<SubscriptionTier>): StoreProductQueryOutcome
    suspend fun purchase(productId: String): StorePurchaseOutcome
    suspend fun restorePurchases(): StoreRestoreOutcome
    suspend fun finalizeConsumable(productId: ProductId, token: PurchaseToken): Boolean
}
