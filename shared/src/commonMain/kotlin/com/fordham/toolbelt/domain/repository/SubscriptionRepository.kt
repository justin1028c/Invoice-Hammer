package com.fordham.toolbelt.domain.repository

import com.fordham.toolbelt.domain.model.subscription.SubscriptionCatalogOutcome
import com.fordham.toolbelt.domain.model.subscription.SubscriptionTier
import com.fordham.toolbelt.domain.model.subscription.SubscriptionFeature
import com.fordham.toolbelt.domain.model.subscription.SubscriptionPurchaseOutcome
import com.fordham.toolbelt.domain.model.subscription.SubscriptionRestoreOutcome
import com.fordham.toolbelt.domain.model.subscription.SubscriptionTierId
import com.fordham.toolbelt.domain.model.subscription.UserEntitlement
import kotlinx.coroutines.flow.Flow

interface SubscriptionRepository {
    val entitlement: Flow<UserEntitlement>

    /** Latest cached entitlement (updated after sync/purchase/restore). */
    fun peekEntitlement(): UserEntitlement

    fun paidTiers(): List<SubscriptionTier>
    suspend fun refreshCatalog(): SubscriptionCatalogOutcome
    suspend fun syncEntitlementFromSupabase(): UserEntitlement
    suspend fun purchase(tierId: SubscriptionTierId): SubscriptionPurchaseOutcome
    suspend fun restorePurchases(): SubscriptionRestoreOutcome

    fun hasFeature(feature: SubscriptionFeature): Boolean
}
