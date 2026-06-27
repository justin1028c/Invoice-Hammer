package com.fordham.toolbelt.domain.model.subscription

import com.fordham.toolbelt.domain.model.FailureMessage
import kotlin.jvm.JvmInline

@JvmInline
value class SubscriptionTierId(val value: String)

enum class SubscriptionFeature {
    AiAgent,
    ReceiptOcr,
    TaxExport,
    BentoReports,
    ForemanAgent,
    /** Bluetooth/USB physical card readers (Stripe Terminal, etc.). */
    BluetoothCardReader,
    /** Saved cards, auto-charge schedules, subscription billing. */
    RecurringCardBilling,
    /** Accelerated settlement / instant payout rails. */
    InstantPayouts
}

enum class SubscriptionBillingPeriod {
    None,
    Monthly,
    Yearly
}

enum class EntitlementSource {
    Free,
    GooglePlay,
    AppleAppStore,
    Supabase,
    Manual
}

data class SubscriptionTier(
    val id: SubscriptionTierId,
    val displayName: String,
    val description: String,
    val sortOrder: Int,
    val googlePlayProductId: String?,
    val appleProductId: String?,
    val billingPeriod: SubscriptionBillingPeriod,
    val priceLabel: String,
    val enabledFeatures: Set<SubscriptionFeature>,
    val isActive: Boolean = true
) {
    val isPaidTier: Boolean get() = id.value != "free"

    fun hasFeature(feature: SubscriptionFeature): Boolean = feature in enabledFeatures
}

data class UserEntitlement(
    val tierId: SubscriptionTierId,
    val source: EntitlementSource,
    val expiresAtMillis: Long? = null,
    val enabledFeatures: Set<SubscriptionFeature>
) {
    val isPro: Boolean get() = enabledFeatures.isNotEmpty() && tierId.value != "free"

    fun hasFeature(feature: SubscriptionFeature): Boolean = feature in enabledFeatures
}

sealed interface SubscriptionCatalogOutcome {
    data class Success(val tiers: List<SubscriptionTier>) : SubscriptionCatalogOutcome
    data class Failure(val error: FailureMessage) : SubscriptionCatalogOutcome
}

sealed interface UserEntitlementOutcome {
    data class Success(val entitlement: UserEntitlement) : UserEntitlementOutcome
    data class Failure(val error: FailureMessage) : UserEntitlementOutcome
}

sealed interface SubscriptionPurchaseOutcome {
    data class Success(val entitlement: UserEntitlement) : SubscriptionPurchaseOutcome
    data object Cancelled : SubscriptionPurchaseOutcome
    data class Failure(val error: FailureMessage) : SubscriptionPurchaseOutcome
}

sealed interface SubscriptionRestoreOutcome {
    data class Success(val entitlement: UserEntitlement) : SubscriptionRestoreOutcome
    data class Failure(val error: FailureMessage) : SubscriptionRestoreOutcome
}
