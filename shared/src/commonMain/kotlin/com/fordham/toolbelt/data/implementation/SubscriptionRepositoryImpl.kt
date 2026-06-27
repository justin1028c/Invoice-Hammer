package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.remote.SupabaseEntitlementUpsertOutcome
import com.fordham.toolbelt.data.remote.SupabaseEntitlementUpsertRequest
import com.fordham.toolbelt.data.remote.SupabaseSubscriptionClient
import com.fordham.toolbelt.data.remote.SupabaseSubscriptionTiersOutcome
import com.fordham.toolbelt.data.remote.SupabaseUserEntitlementFetchOutcome
import com.fordham.toolbelt.domain.model.BusinessSettings
import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.model.subscription.EntitlementSource
import com.fordham.toolbelt.domain.model.subscription.SubscriptionCatalogOutcome
import com.fordham.toolbelt.domain.model.subscription.SubscriptionFeature
import com.fordham.toolbelt.domain.model.subscription.SubscriptionPurchaseOutcome
import com.fordham.toolbelt.domain.model.subscription.SubscriptionRestoreOutcome
import com.fordham.toolbelt.domain.model.subscription.SubscriptionTier
import com.fordham.toolbelt.domain.model.subscription.SubscriptionTierId
import com.fordham.toolbelt.domain.model.subscription.UserEntitlement
import com.fordham.toolbelt.domain.repository.AuthRepository
import com.fordham.toolbelt.domain.repository.SettingsRepository
import com.fordham.toolbelt.domain.repository.StoreBillingGateway
import com.fordham.toolbelt.domain.repository.StorePurchaseOutcome
import com.fordham.toolbelt.domain.repository.StoreRestoreOutcome
import com.fordham.toolbelt.domain.repository.SubscriptionRepository
import com.fordham.toolbelt.util.PlatformTarget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock

class SubscriptionRepositoryImpl(
    private val supabaseSubscriptionClient: SupabaseSubscriptionClient,
    private val storeBillingGateway: StoreBillingGateway,
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
    private val platformTarget: PlatformTarget
) : SubscriptionRepository {

    private val catalog = MutableStateFlow(defaultCatalog())
    private val _entitlement = MutableStateFlow(freeEntitlement())
    override val entitlement: Flow<UserEntitlement> = _entitlement.asStateFlow()

    override fun peekEntitlement(): UserEntitlement = _entitlement.value

    override suspend fun refreshCatalog(): SubscriptionCatalogOutcome {
        return when (val outcome = supabaseSubscriptionClient.fetchActiveTiers()) {
            is SupabaseSubscriptionTiersOutcome.Success -> {
                val tiers = outcome.tiers.map { SubscriptionTierMapper.fromDto(it) }
                    .ifEmpty { defaultCatalog() }
                catalog.value = tiers
                SubscriptionCatalogOutcome.Success(tiers)
            }
            SupabaseSubscriptionTiersOutcome.NotConfigured -> {
                val tiers = defaultCatalog()
                catalog.value = tiers
                SubscriptionCatalogOutcome.Success(tiers)
            }
            is SupabaseSubscriptionTiersOutcome.Failure ->
                SubscriptionCatalogOutcome.Failure(outcome.error)
        }
    }

    override suspend fun syncEntitlementFromSupabase(): UserEntitlement {
        val userId = authRepository.currentUser.first()?.id?.value
        if (userId == null) {
            applyEntitlement(freeEntitlement())
            return _entitlement.value
        }

        when (val remote = supabaseSubscriptionClient.fetchUserEntitlement(userId)) {
            is SupabaseUserEntitlementFetchOutcome.Success -> {
                val tier = catalog.value.find { it.id.value == remote.entitlement.tierId }
                    ?: SubscriptionTierMapper.proTierFallback()
                val entitlement = UserEntitlement(
                    tierId = SubscriptionTierId(remote.entitlement.tierId),
                    source = remote.entitlement.source.toEntitlementSource(),
                    expiresAtMillis = remote.entitlement.expiresAtMillis,
                    enabledFeatures = tier.enabledFeatures
                )
                applyEntitlement(entitlement)
            }
            SupabaseUserEntitlementFetchOutcome.NotFound,
            SupabaseUserEntitlementFetchOutcome.NotConfigured ->
                applyEntitlement(freeEntitlement())
            is SupabaseUserEntitlementFetchOutcome.Failure -> applyEntitlement(freeEntitlement())
        }
        return _entitlement.value
    }

    override suspend fun purchase(tierId: SubscriptionTierId): SubscriptionPurchaseOutcome {
        val tier = catalog.value.find { it.id == tierId }
            ?: return SubscriptionPurchaseOutcome.Failure(FailureMessage("Subscription tier not found."))

        val productId = when (platformTarget) {
            PlatformTarget.Android -> tier.googlePlayProductId
            PlatformTarget.Ios -> tier.appleProductId
        } ?: return SubscriptionPurchaseOutcome.Failure(
            FailureMessage("No ${platformTarget.name} product id configured for ${tier.displayName}.")
        )

        return when (val outcome = storeBillingGateway.purchase(productId)) {
            StorePurchaseOutcome.Cancelled -> SubscriptionPurchaseOutcome.Cancelled
            is StorePurchaseOutcome.Failure -> SubscriptionPurchaseOutcome.Failure(outcome.error)
            is StorePurchaseOutcome.Success -> {
                val matchedTier = SubscriptionTierMapper.tierForStoreProduct(
                    catalog.value,
                    outcome.result.productId,
                    platformTarget == PlatformTarget.Android
                ) ?: tier
                val entitlement = UserEntitlement(
                    tierId = matchedTier.id,
                    source = when (platformTarget) {
                        PlatformTarget.Android -> EntitlementSource.GooglePlay
                        PlatformTarget.Ios -> EntitlementSource.AppleAppStore
                    },
                    enabledFeatures = matchedTier.enabledFeatures
                )
                persistEntitlement(entitlement, outcome.result.purchaseToken)
                applyEntitlement(entitlement)
                SubscriptionPurchaseOutcome.Success(entitlement)
            }
        }
    }

    override suspend fun restorePurchases(): SubscriptionRestoreOutcome {
        return when (val outcome = storeBillingGateway.restorePurchases()) {
            is StoreRestoreOutcome.Failure -> SubscriptionRestoreOutcome.Failure(outcome.error)
            is StoreRestoreOutcome.Success -> {
                val productId = outcome.activeProductIds.firstOrNull()
                if (productId == null) {
                    applyEntitlement(freeEntitlement())
                    return SubscriptionRestoreOutcome.Success(_entitlement.value)
                }
                val tier = SubscriptionTierMapper.tierForStoreProduct(
                    catalog.value,
                    productId,
                    platformTarget == PlatformTarget.Android
                ) ?: when (productId) {
                    "invoice_hammer_founder_lifetime" -> SubscriptionTierMapper.founderLifetimeFallback()
                    "invoice_hammer_pro_yearly" -> SubscriptionTierMapper.proYearlyFallback()
                    else -> SubscriptionTierMapper.proTierFallback()
                }
                val entitlement = UserEntitlement(
                    tierId = tier.id,
                    source = when (platformTarget) {
                        PlatformTarget.Android -> EntitlementSource.GooglePlay
                        PlatformTarget.Ios -> EntitlementSource.AppleAppStore
                    },
                    enabledFeatures = tier.enabledFeatures
                )
                persistEntitlement(entitlement, purchaseToken = null)
                applyEntitlement(entitlement)
                SubscriptionRestoreOutcome.Success(entitlement)
            }
        }
    }

    override fun hasFeature(feature: SubscriptionFeature): Boolean {
        val settings = try {
            kotlinx.coroutines.runBlocking { settingsRepository.getBusinessSettings() }
        } catch (e: Exception) {
            return false
        }
        return settings.isPremium || _entitlement.value.hasFeature(feature)
    }

    override fun paidTiers(): List<SubscriptionTier> =
        catalog.value.filter { it.isPaidTier && it.isActive }

    fun currentCatalog(): List<SubscriptionTier> = catalog.value

    private suspend fun persistEntitlement(entitlement: UserEntitlement, purchaseToken: String?) {
        val userId = authRepository.currentUser.first()?.id?.value ?: return
        val source = when (entitlement.source) {
            EntitlementSource.GooglePlay -> "google_play"
            EntitlementSource.AppleAppStore -> "apple_app_store"
            EntitlementSource.Supabase -> "supabase"
            EntitlementSource.Manual -> "manual"
            EntitlementSource.Free -> "free"
        }
        supabaseSubscriptionClient.upsertUserEntitlement(
            SupabaseEntitlementUpsertRequest(
                userId = userId,
                tierId = entitlement.tierId.value,
                source = source,
                purchaseToken = purchaseToken,
                expiresAtMillis = entitlement.expiresAtMillis,
                updatedAtMillis = Clock.System.now().toEpochMilliseconds()
            )
        )
    }

    private suspend fun applyEntitlement(entitlement: UserEntitlement) {
        _entitlement.update { entitlement }
        val settings = settingsRepository.getBusinessSettings()
        if (entitlement.isPro || !settings.isPremium) {
            settingsRepository.saveBusinessSettings(settings.copy(isPremium = entitlement.isPro))
        }
    }

    private fun defaultCatalog(): List<SubscriptionTier> = listOf(
        SubscriptionTierMapper.freeTier(),
        SubscriptionTierMapper.proTierFallback(),
        SubscriptionTierMapper.proYearlyFallback(),
        SubscriptionTierMapper.founderLifetimeFallback()
    )

    private fun freeEntitlement(): UserEntitlement = UserEntitlement(
        tierId = SubscriptionTierId("free"),
        source = EntitlementSource.Free,
        enabledFeatures = emptySet()
    )

    private fun String.toEntitlementSource(): EntitlementSource = when (lowercase()) {
        "google_play" -> EntitlementSource.GooglePlay
        "apple_app_store", "apple" -> EntitlementSource.AppleAppStore
        "supabase" -> EntitlementSource.Supabase
        "manual" -> EntitlementSource.Manual
        else -> EntitlementSource.Free
    }
}
