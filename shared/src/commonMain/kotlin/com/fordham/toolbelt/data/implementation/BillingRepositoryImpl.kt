package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.remote.SupabaseEntitlementUpsertOutcome
import com.fordham.toolbelt.data.remote.SupabaseEntitlementUpsertRequest
import com.fordham.toolbelt.data.remote.SupabaseSubscriptionClient
import com.fordham.toolbelt.data.remote.SupabaseSubscriptionTiersOutcome
import com.fordham.toolbelt.data.remote.SupabaseUserEntitlementFetchOutcome
import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.model.BusinessSettings
import com.fordham.toolbelt.domain.model.subscription.*
import com.fordham.toolbelt.domain.repository.*
import com.fordham.toolbelt.util.PlatformTarget
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class BillingRepositoryImpl(
    private val supabaseSubscriptionClient: SupabaseSubscriptionClient,
    private val storeBillingGateway: StoreBillingGateway,
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
    private val platformTarget: PlatformTarget,
    private val httpClient: HttpClient,
    private val supabaseConfig: com.fordham.toolbelt.data.remote.SupabaseConfig
) : BillingRepository {

    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
    private val catalog = MutableStateFlow(defaultCatalog())
    private val _entitlement = MutableStateFlow(freeEntitlement())

    init {
        // Pillar 3: Automatic Startup Purchase & Token Reconciler
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            try {
                reconcileTokens()
            } catch (_: Exception) {}
        }
    }
    override val entitlement: Flow<UserEntitlement> = _entitlement.asStateFlow()

    override val tokenBalances: Flow<Map<PremiumFeature, TokenCount>> =
        combine(
            settingsRepository.businessSettingsFlow,
            _entitlement
        ) { settings, entitlement ->
            val isPro = entitlement.isPro || settings.isPremium
            val foremanAgentTokens = if (isPro) {
                val remainingQuota = (500 - settings.aiActionsUsedThisMonth).coerceAtLeast(0)
                if (remainingQuota > 0) remainingQuota else settings.hammerCredits / 2
            } else {
                settings.hammerCredits / 2
            }
            mapOf(
                PremiumFeature.AI_RECEIPT_SCAN to TokenCount(settings.hammerCredits),
                PremiumFeature.INVOICE_GENERATION to TokenCount(settings.hammerCredits),
                PremiumFeature.TAX_PACKET_EXPORT to TokenCount(settings.hammerCredits),
                PremiumFeature.CLOUD_SYNC to TokenCount(0),
                PremiumFeature.BUSINESS_ANALYTICS to TokenCount(0),
                PremiumFeature.FOREMAN_AGENT to TokenCount(foremanAgentTokens),
                PremiumFeature.UNLIMITED_CLIENTS to TokenCount(0)
            )
        }.distinctUntilChanged()

    override suspend fun queryCatalog(): BillingCatalogOutcome {
        val tiers = when (val outcome = supabaseSubscriptionClient.fetchActiveTiers()) {
            is SupabaseSubscriptionTiersOutcome.Success -> {
                outcome.tiers.map { SubscriptionTierMapper.fromDto(it) }
                    .ifEmpty { defaultCatalog() }
            }
            else -> defaultCatalog()
        }
        catalog.value = tiers

        val products = listOf(
            PurchasableProduct.HammerCreditPack50,
            PurchasableProduct.HammerCreditPack150,
            PurchasableProduct.HammerCreditPack400,
            PurchasableProduct.ProMonthly,
            PurchasableProduct.ProYearly
        )
        return BillingCatalogOutcome.Success(products)
    }

    override suspend fun purchaseProduct(product: PurchasableProduct): BillingOutcome {
        val productId = when (platformTarget) {
            PlatformTarget.Android -> product.googlePlayProductId.value
            PlatformTarget.Ios -> product.appleProductId.value
        }

        return when (val outcome = storeBillingGateway.purchase(productId)) {
            StorePurchaseOutcome.Cancelled -> BillingOutcome.Cancelled
            is StorePurchaseOutcome.Failure -> BillingOutcome.Failure(outcome.error)
            is StorePurchaseOutcome.Success -> {
                // If it is a consumable, increment tokens securely only after backend verification and store finalization
                when (product) {
                    PurchasableProduct.HammerCreditPack50,
                    PurchasableProduct.HammerCreditPack150,
                    PurchasableProduct.HammerCreditPack400 -> {
                        val token = outcome.result.purchaseToken
                        if (token.isNullOrBlank()) {
                            return BillingOutcome.Failure(FailureMessage("Cryptographic purchase verification token is missing."))
                        }
                        val verification = verifyConsumablePurchaseOnServer(product.productId.value, token)
                        if (verification is ServerVerificationResult.Success) {
                            // Enforce the secure zero-drop cryptographic transaction handshake loop:
                            // We consume/finalize the purchase on the store client ONLY after server approval!
                            val isPlatformFinalized = storeBillingGateway.finalizeConsumable(
                                product.productId,
                                PurchaseToken(token)
                            )
                            if (isPlatformFinalized) {
                                addTokens(verification.creditedAmount)
                            } else {
                                return BillingOutcome.Failure(FailureMessage("Server approved transaction, but local store consumption execution failed."))
                            }
                        } else {
                            return BillingOutcome.Failure(FailureMessage("Transaction validation failed on secure server."))
                        }
                    }
                    PurchasableProduct.ProMonthly, PurchasableProduct.ProYearly -> {
                        val isYearly = product is PurchasableProduct.ProYearly
                        val matchedTier = catalog.value.find { 
                            if (isYearly) it.id.value == "pro_yearly" else it.id.value == "pro" 
                        } ?: SubscriptionTierMapper.proTierFallback()
                        
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
                    }
                }
                reconcileTokens()
                BillingOutcome.Success(_entitlement.value)
            }
        }
    }

    override suspend fun restorePurchases(): BillingOutcome {
        return when (val outcome = storeBillingGateway.restorePurchases()) {
            is StoreRestoreOutcome.Failure -> BillingOutcome.Failure(outcome.error)
            is StoreRestoreOutcome.Success -> {
                val productId = outcome.activeProductIds.firstOrNull()
                if (productId == null) {
                    applyEntitlement(freeEntitlement())
                    return BillingOutcome.Success(_entitlement.value)
                }
                val tier = SubscriptionTierMapper.tierForStoreProduct(
                    catalog.value,
                    productId,
                    platformTarget == PlatformTarget.Android
                ) ?: SubscriptionTierMapper.proTierFallback()
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
                BillingOutcome.Success(entitlement)
            }
        }
    }

    override suspend fun consumeToken(feature: PremiumFeature): TokenConsumptionOutcome {
        val settings = settingsRepository.getBusinessSettings()
        val isPro = _entitlement.value.isPro || settings.isPremium

        if (isPro) {
            when (feature) {
                PremiumFeature.FOREMAN_AGENT -> {
                    return if (settings.aiActionsUsedThisMonth < 500) {
                        val updated = settings.copy(aiActionsUsedThisMonth = settings.aiActionsUsedThisMonth + 1)
                        settingsRepository.saveBusinessSettings(updated)
                        reconcileTokens()
                        TokenConsumptionOutcome.Success(TokenCount((500 - updated.aiActionsUsedThisMonth).coerceAtLeast(0)))
                    } else {
                        // Beyond monthly cap, consume from hammerCredits (cost = 2 credits)
                        if (settings.hammerCredits < 2) {
                            TokenConsumptionOutcome.InsufficientTokens(feature)
                        } else {
                            val updated = settings.copy(hammerCredits = settings.hammerCredits - 2)
                            settingsRepository.saveBusinessSettings(updated)
                            reconcileTokens()
                            TokenConsumptionOutcome.Success(TokenCount(updated.hammerCredits))
                        }
                    }
                }
                PremiumFeature.AI_RECEIPT_SCAN,
                PremiumFeature.INVOICE_GENERATION,
                PremiumFeature.TAX_PACKET_EXPORT -> {
                    // Pro has unlimited transactional features, so no consumption of credits.
                    return TokenConsumptionOutcome.Success(TokenCount(settings.hammerCredits))
                }
                else -> {
                    return TokenConsumptionOutcome.Success(TokenCount(0))
                }
            }
        } else {
            // Casual user: consumes from hammerCredits
            val cost = if (feature == PremiumFeature.FOREMAN_AGENT) 2 else 1
            if (settings.hammerCredits < cost) {
                return TokenConsumptionOutcome.InsufficientTokens(feature)
            }
            val updated = settings.copy(hammerCredits = settings.hammerCredits - cost)
            settingsRepository.saveBusinessSettings(updated)
            reconcileTokens()
            return TokenConsumptionOutcome.Success(TokenCount(updated.hammerCredits))
        }
    }

    override suspend fun reconcileTokens(): TokenReconciliationOutcome {
        val settings = settingsRepository.getBusinessSettings()
        val isPro = _entitlement.value.isPro || settings.isPremium
        val foremanAgentTokens = if (isPro) {
            val remainingQuota = (500 - settings.aiActionsUsedThisMonth).coerceAtLeast(0)
            if (remainingQuota > 0) remainingQuota else settings.hammerCredits / 2
        } else {
            settings.hammerCredits / 2
        }
        val balances = mapOf(
            PremiumFeature.AI_RECEIPT_SCAN to TokenCount(settings.hammerCredits),
            PremiumFeature.INVOICE_GENERATION to TokenCount(settings.hammerCredits),
            PremiumFeature.TAX_PACKET_EXPORT to TokenCount(settings.hammerCredits),
            PremiumFeature.CLOUD_SYNC to TokenCount(0),
            PremiumFeature.BUSINESS_ANALYTICS to TokenCount(0),
            PremiumFeature.FOREMAN_AGENT to TokenCount(foremanAgentTokens),
            PremiumFeature.UNLIMITED_CLIENTS to TokenCount(0)
        )
        return TokenReconciliationOutcome.Success(balances)
    }

    override fun hasFeatureAccess(feature: PremiumFeature): Boolean {
        val settings = try {
            kotlinx.coroutines.runBlocking { settingsRepository.getBusinessSettings() }
        } catch (e: Exception) {
            return false
        }

        val isPro = _entitlement.value.isPro || settings.isPremium

        return when (feature) {
            PremiumFeature.AI_RECEIPT_SCAN,
            PremiumFeature.INVOICE_GENERATION,
            PremiumFeature.TAX_PACKET_EXPORT -> {
                isPro || settings.hammerCredits > 0
            }
            PremiumFeature.FOREMAN_AGENT -> {
                if (isPro) {
                    settings.aiActionsUsedThisMonth < 500 || settings.hammerCredits >= 2
                } else {
                    settings.hammerCredits >= 2
                }
            }
            PremiumFeature.CLOUD_SYNC,
            PremiumFeature.BUSINESS_ANALYTICS,
            PremiumFeature.UNLIMITED_CLIENTS -> {
                isPro
            }
        }
    }

    private suspend fun addTokens(count: Int) {
        val settings = settingsRepository.getBusinessSettings()
        val updated = settings.copy(hammerCredits = settings.hammerCredits + count)
        settingsRepository.saveBusinessSettings(updated)
    }

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
        settingsRepository.saveBusinessSettings(settings.copy(isPremium = entitlement.isPro))
    }

    private fun defaultCatalog(): List<SubscriptionTier> = listOf(
        SubscriptionTierMapper.freeTier(),
        SubscriptionTierMapper.proTierFallback(),
        SubscriptionTierMapper.proYearlyFallback()
    )

    private fun freeEntitlement(): UserEntitlement = UserEntitlement(
        tierId = SubscriptionTierId("free"),
        source = EntitlementSource.Free,
        enabledFeatures = emptySet()
    )

    private suspend fun verifyConsumablePurchaseOnServer(
        productId: String,
        purchaseToken: String
    ): ServerVerificationResult {
        // Enforce the Clean Architecture Firewall:
        // Pass purchaseToken (Google Play Purchase Token or Apple JWS String) to secure server.
        // The server validates transaction integrity and updates DB.
        val userId = authRepository.currentUser.firstOrNull()?.id?.value 
            ?: return ServerVerificationResult.Failure
        
        // Developer Local/Demo Bypass Fallback:
        // If Supabase is unconfigured (development or local test mode), we fall back gracefully.
        if (!supabaseConfig.isConfigured) {
            val amount = when (productId) {
                "hammer_credit_pack_50" -> 50
                "hammer_credit_pack_150" -> 150
                "hammer_credit_pack_400" -> 350
                else -> 0
            }
            return ServerVerificationResult.Success(amount)
        }

        return try {
            val verifyUrl = "${supabaseConfig.normalizedProjectUrl}/functions/v1/verify-purchase"
            val response = httpClient.post(verifyUrl) {
                header("apikey", supabaseConfig.anonKey)
                header("Authorization", "Bearer ${supabaseConfig.anonKey}")
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(VerifyPurchaseRequest(userId, productId, purchaseToken)))
            }
            
            if (response.status.isSuccess()) {
                val resBody = json.decodeFromString<VerifyPurchaseResponse>(response.bodyAsText())
                if (resBody.success) {
                    ServerVerificationResult.Success(resBody.creditedAmount)
                } else {
                    ServerVerificationResult.Failure
                }
            } else {
                ServerVerificationResult.Failure
            }
        } catch (e: Exception) {
            com.fordham.toolbelt.util.AppLogger.e("BillingRepository", "Supabase cryptographic verification failed", e)
            ServerVerificationResult.Failure
        }
    }
}

@Serializable
private data class VerifyPurchaseRequest(
    val userId: String,
    val productId: String,
    val purchaseToken: String
)

@Serializable
private data class VerifyPurchaseResponse(
    val success: Boolean,
    val creditedAmount: Int,
    val message: String = ""
)

private sealed interface ServerVerificationResult {
    data class Success(val creditedAmount: Int) : ServerVerificationResult
    data object Failure : ServerVerificationResult
}
