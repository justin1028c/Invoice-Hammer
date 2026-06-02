package com.fordham.toolbelt.domain.usecase.subscription

import com.fordham.toolbelt.domain.model.subscription.*
import com.fordham.toolbelt.domain.repository.BillingRepository
import kotlinx.coroutines.flow.Flow

class EvaluateFeatureAccessUseCase(
    private val billingRepository: BillingRepository
) {
    operator fun invoke(feature: PremiumFeature): Boolean =
        billingRepository.hasFeatureAccess(feature)
}

class ConsumeTokenUseCase(
    private val billingRepository: BillingRepository
) {
    suspend operator fun invoke(feature: PremiumFeature): TokenConsumptionOutcome =
        billingRepository.consumeToken(feature)
}

class PurchaseProductUseCase(
    private val billingRepository: BillingRepository
) {
    suspend operator fun invoke(product: PurchasableProduct): BillingOutcome =
        billingRepository.purchaseProduct(product)
}

class ReconcileTokensUseCase(
    private val billingRepository: BillingRepository
) {
    suspend operator fun invoke(): TokenReconciliationOutcome =
        billingRepository.reconcileTokens()
}

class ObserveTokenBalancesUseCase(
    private val billingRepository: BillingRepository
) {
    operator fun invoke(): Flow<Map<PremiumFeature, TokenCount>> =
        billingRepository.tokenBalances
}
