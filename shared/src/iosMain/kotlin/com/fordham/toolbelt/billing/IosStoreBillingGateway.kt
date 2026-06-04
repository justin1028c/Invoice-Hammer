package com.fordham.toolbelt.billing

import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.model.subscription.ProductId
import com.fordham.toolbelt.domain.model.subscription.PurchaseToken
import com.fordham.toolbelt.domain.model.subscription.SubscriptionTier
import com.fordham.toolbelt.domain.repository.StoreBillingGateway
import com.fordham.toolbelt.domain.repository.StoreProductQueryOutcome
import com.fordham.toolbelt.domain.repository.StorePurchaseOutcome
import com.fordham.toolbelt.domain.repository.StorePurchaseResult
import com.fordham.toolbelt.domain.repository.StoreRestoreOutcome

class IosStoreBillingGateway : StoreBillingGateway {
    private val consumableProductIds = setOf(
        "scan_token_pack_15",
        "scan_token_pack_40",
        "invoice_pack_10",
        "tax_export_oneshot",
        "hammer_credit_pack_50",
        "hammer_credit_pack_150",
        "hammer_credit_pack_400"
    )

    override suspend fun queryProductIds(tiers: List<SubscriptionTier>): StoreProductQueryOutcome {
        val subscriptionIds = tiers.mapNotNull { it.appleProductId }.distinct()
        val consumableIds = consumableProductIds.toList()
        val allIds = (subscriptionIds + consumableIds).distinct()
        return if (allIds.isEmpty()) {
            StoreProductQueryOutcome.Failure(FailureMessage("No App Store product IDs configured."))
        } else {
            StoreProductQueryOutcome.Success(allIds)
        }
    }

    override suspend fun purchase(productId: String): StorePurchaseOutcome {
        val bridge = IosStoreBillingServiceProvider.bridge
            ?: return StorePurchaseOutcome.Failure(
                FailureMessage("App Store billing bridge is not registered on iOS.")
            )
        val result = bridge.purchase(productId)
        return when {
            result.cancelled -> StorePurchaseOutcome.Cancelled
            result.success -> StorePurchaseOutcome.Success(
                StorePurchaseResult(
                    productId = result.productId ?: productId,
                    purchaseToken = result.transactionId,
                    isAcknowledged = true
                )
            )
            else -> StorePurchaseOutcome.Failure(
                FailureMessage(result.errorMessage ?: "App Store purchase failed.")
            )
        }
    }

    override suspend fun restorePurchases(): StoreRestoreOutcome {
        val bridge = IosStoreBillingServiceProvider.bridge
            ?: return StoreRestoreOutcome.Failure(
                FailureMessage("App Store billing bridge is not registered on iOS.")
            )
        val result = bridge.restorePurchases()
        return if (result.success) {
            StoreRestoreOutcome.Success(result.activeProductIds)
        } else {
            StoreRestoreOutcome.Failure(
                FailureMessage(result.errorMessage ?: "App Store restore failed.")
            )
        }
    }

    override suspend fun finalizeConsumable(productId: ProductId, token: PurchaseToken): Boolean {
        if (!consumableProductIds.contains(productId.value)) return false
        val bridge = IosStoreBillingServiceProvider.bridge ?: return true
        return bridge.finishTransaction(token.value)
    }
}
