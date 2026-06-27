package com.fordham.toolbelt.domain.model.subscription

import com.fordham.toolbelt.domain.model.FailureMessage
import kotlin.jvm.JvmInline

@JvmInline
value class TokenCount(val value: Int) {
    init {
        require(value >= 0) { "Token count cannot be negative." }
    }
}

@JvmInline
value class ProductId(val value: String) {
    init {
        require(value.isNotBlank()) { "Product ID cannot be blank." }
    }
}

@JvmInline
value class PurchaseToken(val value: String) {
    init {
        require(value.isNotBlank()) { "Purchase token cannot be empty." }
    }
}

enum class PremiumFeature {
    AI_RECEIPT_SCAN,
    INVOICE_GENERATION,
    TAX_PACKET_EXPORT,
    CLOUD_SYNC,
    BUSINESS_ANALYTICS,
    FOREMAN_AGENT,
    UNLIMITED_CLIENTS
}

sealed interface PurchasableProduct {
    val productId: ProductId
    val displayName: String
    val appleProductId: ProductId
    val googlePlayProductId: ProductId

    // Consumables (Hammer Credits)
    data object HammerCreditPack50 : PurchasableProduct {
        override val productId = ProductId("hammer_credit_pack_50")
        override val displayName = "50 Hammer Credits"
        override val appleProductId = ProductId("hammer_credit_pack_50")
        override val googlePlayProductId = ProductId("hammer_credit_pack_50")
    }

    data object HammerCreditPack150 : PurchasableProduct {
        override val productId = ProductId("hammer_credit_pack_150")
        override val displayName = "150 Hammer Credits"
        override val appleProductId = ProductId("hammer_credit_pack_150")
        override val googlePlayProductId = ProductId("hammer_credit_pack_150")
    }

    data object HammerCreditPack400 : PurchasableProduct {
        override val productId = ProductId("hammer_credit_pack_400")
        override val displayName = "400 Hammer Credits"
        override val appleProductId = ProductId("hammer_credit_pack_400")
        override val googlePlayProductId = ProductId("hammer_credit_pack_400")
    }


    // Subscriptions
    data object ProMonthly : PurchasableProduct {
        override val productId = ProductId("invoice_hammer_pro_monthly")
        override val displayName = "Pro Monthly"
        override val appleProductId = ProductId("invoice_hammer_pro_monthly")
        override val googlePlayProductId = ProductId("invoice_hammer_pro_monthly")
    }

    data object ProYearly : PurchasableProduct {
        override val productId = ProductId("invoice_hammer_pro_yearly")
        override val displayName = "Pro Yearly"
        override val appleProductId = ProductId("invoice_hammer_pro_yearly")
        override val googlePlayProductId = ProductId("invoice_hammer_pro_yearly")
    }

    data object FounderLifetime : PurchasableProduct {
        override val productId = ProductId("invoice_hammer_founder_lifetime")
        override val displayName = "Founder's Lifetime Pass"
        override val appleProductId = ProductId("invoice_hammer_founder_lifetime")
        override val googlePlayProductId = ProductId("invoice_hammer_founder_lifetime")
    }
}

sealed interface BillingCatalogOutcome {
    data class Success(val products: List<PurchasableProduct>) : BillingCatalogOutcome
    data class Failure(val error: FailureMessage) : BillingCatalogOutcome
}

sealed interface BillingOutcome {
    data class Success(val activeEntitlement: UserEntitlement) : BillingOutcome
    data object Cancelled : BillingOutcome
    data class Failure(val error: FailureMessage) : BillingOutcome
}

sealed interface TokenConsumptionOutcome {
    data class Success(val remaining: TokenCount) : TokenConsumptionOutcome
    data class InsufficientTokens(val feature: PremiumFeature) : TokenConsumptionOutcome
    data class Failure(val error: FailureMessage) : TokenConsumptionOutcome
}

sealed interface TokenReconciliationOutcome {
    data class Success(val reconciledBalances: Map<PremiumFeature, TokenCount>) : TokenReconciliationOutcome
    data class Failure(val error: FailureMessage) : TokenReconciliationOutcome
}
