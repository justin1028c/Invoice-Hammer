package com.fordham.toolbelt.domain.model.credits

import com.fordham.toolbelt.domain.model.FailureMessage
import kotlin.jvm.JvmInline
import kotlinx.coroutines.flow.Flow

@JvmInline
value class HammerCredits(val value: Int) {
    init { 
        require(value >= 0) { "Token balance cannot be negative." } 
    }

    operator fun minus(other: HammerCredits): HammerCredits {
        val result = this.value - other.value
        return HammerCredits(if (result < 0) 0 else result)
    }

    operator fun plus(other: HammerCredits): HammerCredits {
        return HammerCredits(this.value + other.value)
    }
    
    fun isGreaterThanOrEqual(other: HammerCredits): Boolean {
        return this.value >= other.value
    }
}

sealed interface FeatureCost {
    data object Free : FeatureCost
    @JvmInline value class Cost(val credits: HammerCredits) : FeatureCost
}

sealed interface CreditSpendOutcome {
    data class Success(val remainingBalance: HammerCredits) : CreditSpendOutcome
    data object InsufficientFunds : CreditSpendOutcome
    data class Failure(val error: FailureMessage) : CreditSpendOutcome
}

sealed interface FeatureAccessStatus {
    data object Allowed : FeatureAccessStatus
    data class SpendPromptRequired(val cost: HammerCredits) : FeatureAccessStatus
    data class PaywallRequired(val deficit: HammerCredits) : FeatureAccessStatus
}

interface HammerCreditsRepository {
    val balanceFlow: Flow<HammerCredits>
    suspend fun getBalance(): HammerCredits
    suspend fun spendCredits(cost: HammerCredits, featureName: String): CreditSpendOutcome
    suspend fun buyCreditsPack(packId: String): CreditSpendOutcome
}
