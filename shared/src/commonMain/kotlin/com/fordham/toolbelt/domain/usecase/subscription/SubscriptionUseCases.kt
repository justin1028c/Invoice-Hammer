package com.fordham.toolbelt.domain.usecase.subscription

import com.fordham.toolbelt.domain.model.subscription.SubscriptionCatalogOutcome
import com.fordham.toolbelt.domain.model.subscription.SubscriptionFeature
import com.fordham.toolbelt.domain.model.subscription.SubscriptionPurchaseOutcome
import com.fordham.toolbelt.domain.model.subscription.SubscriptionRestoreOutcome
import com.fordham.toolbelt.domain.model.subscription.SubscriptionTier
import com.fordham.toolbelt.domain.model.subscription.SubscriptionTierId
import com.fordham.toolbelt.domain.model.subscription.UserEntitlement
import com.fordham.toolbelt.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow

class RefreshSubscriptionCatalogUseCase(
    private val subscriptionRepository: SubscriptionRepository
) {
    suspend operator fun invoke(): SubscriptionCatalogOutcome = subscriptionRepository.refreshCatalog()
}

class SyncSubscriptionEntitlementUseCase(
    private val subscriptionRepository: SubscriptionRepository
) {
    suspend operator fun invoke(): UserEntitlement = subscriptionRepository.syncEntitlementFromSupabase()
}

class ObserveUserEntitlementUseCase(
    private val subscriptionRepository: SubscriptionRepository
) {
    operator fun invoke(): Flow<UserEntitlement> = subscriptionRepository.entitlement
}

class GetPaywallTiersUseCase(
    private val subscriptionRepository: SubscriptionRepository
) {
    operator fun invoke(): List<SubscriptionTier> = subscriptionRepository.paidTiers()
}

class PurchaseSubscriptionTierUseCase(
    private val subscriptionRepository: SubscriptionRepository
) {
    suspend operator fun invoke(tierId: SubscriptionTierId): SubscriptionPurchaseOutcome =
        subscriptionRepository.purchase(tierId)
}

class RestoreSubscriptionPurchasesUseCase(
    private val subscriptionRepository: SubscriptionRepository
) {
    suspend operator fun invoke(): SubscriptionRestoreOutcome = subscriptionRepository.restorePurchases()
}

class HasSubscriptionFeatureUseCase(
    private val subscriptionRepository: SubscriptionRepository
) {
    operator fun invoke(feature: SubscriptionFeature): Boolean = subscriptionRepository.hasFeature(feature)
}
