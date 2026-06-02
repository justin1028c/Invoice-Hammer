package com.fordham.toolbelt.domain.repository

import com.fordham.toolbelt.domain.model.subscription.*
import kotlinx.coroutines.flow.Flow

interface BillingRepository {
    /**
     * Flow of the user's active subscription entitlement.
     */
    val entitlement: Flow<UserEntitlement>

    /**
     * Flow of the user's consumable token balances per premium feature category.
     */
    val tokenBalances: Flow<Map<PremiumFeature, TokenCount>>

    /**
     * Retrieves all active purchasable products in the catalog.
     */
    suspend fun queryCatalog(): BillingCatalogOutcome

    /**
     * Launches purchase flow for either a monthly subscription or consumable token pack.
     */
    suspend fun purchaseProduct(product: PurchasableProduct): BillingOutcome

    /**
     * Restores all platform store purchases and syncs active subscriptions.
     */
    suspend fun restorePurchases(): BillingOutcome

    /**
     * Deducts one token for the specified premium feature.
     * Guaranteed to terminate locally and synchronize with Supabase backup securely.
     */
    suspend fun consumeToken(feature: PremiumFeature): TokenConsumptionOutcome

    /**
     * Synchronizes and reconciles local token counts with remote Supabase backup.
     */
    suspend fun reconcileTokens(): TokenReconciliationOutcome

    /**
     * Evaluation engine checking if user has premium access either via subscription OR positive token balance.
     */
    fun hasFeatureAccess(feature: PremiumFeature): Boolean
}
