package com.fordham.toolbelt.domain.usecase.credits

import com.fordham.toolbelt.domain.model.credits.*
import com.fordham.toolbelt.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class EvaluateHammerCreditsAccessUseCase(
    private val subRepository: SubscriptionRepository,
    private val creditsRepository: HammerCreditsRepository
) {
    /**
     * Executes a clean, snapshot evaluation of feature availability.
     */
    suspend fun evaluateOnce(featureCost: FeatureCost): FeatureAccessStatus {
        if (featureCost is FeatureCost.Free) return FeatureAccessStatus.Allowed
        
        if (subRepository.peekEntitlement().isPro) {
            return FeatureAccessStatus.Allowed
        }
        
        val currentBalance = creditsRepository.getBalance()
        val required = (featureCost as FeatureCost.Cost).credits
        
        return if (currentBalance.isGreaterThanOrEqual(required)) {
            FeatureAccessStatus.SpendPromptRequired(required)
        } else {
            FeatureAccessStatus.PaywallRequired(required - currentBalance)
        }
    }

    /**
     * Exposes a reactive stream combining subscription state and credit balances 
     * to eliminate event dropping or stale UI presentation.
     */
    fun observeAccess(featureCost: FeatureCost): Flow<FeatureAccessStatus> {
        return subRepository.entitlement.combine(creditsRepository.balanceFlow) { entitlement, balance ->
            if (featureCost is FeatureCost.Free) return@combine FeatureAccessStatus.Allowed
            if (entitlement.isPro) return@combine FeatureAccessStatus.Allowed
            
            val required = (featureCost as FeatureCost.Cost).credits
            if (balance.isGreaterThanOrEqual(required)) {
                FeatureAccessStatus.SpendPromptRequired(required)
            } else {
                FeatureAccessStatus.PaywallRequired(required - balance)
            }
        }
    }
}
