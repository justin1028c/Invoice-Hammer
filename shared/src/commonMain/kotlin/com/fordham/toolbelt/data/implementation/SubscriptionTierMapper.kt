package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.remote.SupabaseSubscriptionTierDto
import com.fordham.toolbelt.domain.model.subscription.SubscriptionBillingPeriod
import com.fordham.toolbelt.domain.model.subscription.SubscriptionFeature
import com.fordham.toolbelt.domain.model.subscription.SubscriptionTier
import com.fordham.toolbelt.domain.model.subscription.SubscriptionTierId
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive

internal object SubscriptionTierMapper {
    fun fromDto(dto: SupabaseSubscriptionTierDto): SubscriptionTier {
        return SubscriptionTier(
            id = SubscriptionTierId(dto.id),
            displayName = dto.displayName,
            description = dto.description,
            sortOrder = dto.sortOrder,
            googlePlayProductId = dto.googlePlayProductId?.takeIf { it.isNotBlank() },
            appleProductId = dto.appleProductId?.takeIf { it.isNotBlank() },
            billingPeriod = dto.billingPeriod.toBillingPeriod(),
            priceLabel = dto.priceLabel,
            enabledFeatures = parseFeatures(dto.features),
            isActive = dto.isActive
        )
    }

    fun freeTier(): SubscriptionTier = SubscriptionTier(
        id = SubscriptionTierId("free"),
        displayName = "Free",
        description = "Core invoicing tools.",
        sortOrder = 0,
        googlePlayProductId = null,
        appleProductId = null,
        billingPeriod = SubscriptionBillingPeriod.None,
        priceLabel = "Free",
        enabledFeatures = emptySet()
    )

    fun proTierFallback(): SubscriptionTier = SubscriptionTier(
        id = SubscriptionTierId("pro_monthly"),
        displayName = "Pro Monthly",
        description = "AI, OCR, Bento reports, and tax export.",
        sortOrder = 1,
        googlePlayProductId = "invoice-hammer-pro-monthly",
        appleProductId = "invoice-hammer-pro-monthly",
        billingPeriod = SubscriptionBillingPeriod.Monthly,
        priceLabel = "$19.99 / mo",
        enabledFeatures = setOf(
            SubscriptionFeature.AiAgent,
            SubscriptionFeature.ReceiptOcr,
            SubscriptionFeature.TaxExport,
            SubscriptionFeature.BentoReports,
            SubscriptionFeature.ForemanAgent,
            SubscriptionFeature.BluetoothCardReader,
            SubscriptionFeature.RecurringCardBilling,
            SubscriptionFeature.InstantPayouts
        )
    )

    fun proYearlyFallback(): SubscriptionTier = SubscriptionTier(
        id = SubscriptionTierId("pro_yearly"),
        displayName = "Pro Yearly",
        // $19.99/mo × 12 = $239.88; yearly saves $79.89
        description = "All Pro features · Save \$79.89 vs monthly",
        sortOrder = 2,
        googlePlayProductId = "invoice-hammer-pro-yearly",
        appleProductId = "invoice-hammer-pro-yearly",
        billingPeriod = SubscriptionBillingPeriod.Yearly,
        priceLabel = "$159.99 / yr",
        enabledFeatures = setOf(
            SubscriptionFeature.AiAgent,
            SubscriptionFeature.ReceiptOcr,
            SubscriptionFeature.TaxExport,
            SubscriptionFeature.BentoReports,
            SubscriptionFeature.ForemanAgent,
            SubscriptionFeature.BluetoothCardReader,
            SubscriptionFeature.RecurringCardBilling,
            SubscriptionFeature.InstantPayouts
        )
    )
    
    fun founderLifetimeFallback(): SubscriptionTier = SubscriptionTier(
        id = SubscriptionTierId("founder_lifetime"),
        displayName = "Founder's Lifetime Pass",
        description = "One-time purchase · All Pro features forever",
        sortOrder = 3,
        googlePlayProductId = "invoice-hammer-founder-lifetime",
        appleProductId = "invoice-hammer-founder-lifetime",
        billingPeriod = SubscriptionBillingPeriod.None,
        priceLabel = "$79.99",
        enabledFeatures = setOf(
            SubscriptionFeature.AiAgent,
            SubscriptionFeature.ReceiptOcr,
            SubscriptionFeature.TaxExport,
            SubscriptionFeature.BentoReports,
            SubscriptionFeature.ForemanAgent,
            SubscriptionFeature.BluetoothCardReader,
            SubscriptionFeature.RecurringCardBilling,
            SubscriptionFeature.InstantPayouts
        )
    )

    private fun parseFeatures(features: JsonObject): Set<SubscriptionFeature> {
        val enabled = mutableSetOf<SubscriptionFeature>()
        if (features.booleanFeature("ai_agent")) enabled += SubscriptionFeature.AiAgent
        if (features.booleanFeature("receipt_ocr")) enabled += SubscriptionFeature.ReceiptOcr
        if (features.booleanFeature("tax_export")) enabled += SubscriptionFeature.TaxExport
        if (features.booleanFeature("bento_reports")) enabled += SubscriptionFeature.BentoReports
        if (features.booleanFeature("foreman_agent")) enabled += SubscriptionFeature.ForemanAgent
        if (features.booleanFeature("bluetooth_card_reader")) enabled += SubscriptionFeature.BluetoothCardReader
        if (features.booleanFeature("recurring_card_billing")) enabled += SubscriptionFeature.RecurringCardBilling
        if (features.booleanFeature("instant_payouts")) enabled += SubscriptionFeature.InstantPayouts
        return enabled
    }

    private fun JsonObject.booleanFeature(key: String): Boolean =
        this[key]?.jsonPrimitive?.booleanOrNull == true

    private fun String.toBillingPeriod(): SubscriptionBillingPeriod = when (lowercase()) {
        "monthly" -> SubscriptionBillingPeriod.Monthly
        "yearly", "annual" -> SubscriptionBillingPeriod.Yearly
        else -> SubscriptionBillingPeriod.None
    }

    fun tierForStoreProduct(
        tiers: List<SubscriptionTier>,
        productId: String,
        isAndroid: Boolean
    ): SubscriptionTier? = tiers.firstOrNull { tier ->
        if (isAndroid) tier.googlePlayProductId == productId else tier.appleProductId == productId
    }
}
