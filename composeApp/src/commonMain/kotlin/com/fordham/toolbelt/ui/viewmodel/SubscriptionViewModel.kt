package com.fordham.toolbelt.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.model.subscription.*
import com.fordham.toolbelt.domain.usecase.subscription.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SubscriptionUiState(
    val entitlement: UserEntitlement? = null,
    val tiers: List<SubscriptionTier> = emptyList(),
    val tokenBalances: Map<PremiumFeature, TokenCount> = emptyMap(),
    val isLoading: Boolean = false,
    val message: String? = null,
    val purchaseSuccess: Boolean = false
) {
    // Unified Hammer Credits balance
    val hammerCreditsCount: Int get() = tokenBalances[PremiumFeature.AI_RECEIPT_SCAN]?.value ?: 0
    
    // Pro monthly AI cap balance
    val foremanAgentCredits: Int get() = tokenBalances[PremiumFeature.FOREMAN_AGENT]?.value ?: 0

    // Backwards-compatible alias for existing layout callers
    val scanTokenCount: Int get() = hammerCreditsCount

    // Soft-pressure nudges & barriers
    val showLowTokensNudge: Boolean get() = !isPro && hammerCreditsCount in 1..3
    val showLowTokensWarning: Boolean get() = !isPro && hammerCreditsCount == 1
    val showHardBarrierPaywall: Boolean get() = !isPro && hammerCreditsCount == 0

    private val isPro: Boolean get() = entitlement?.isPro == true

    val canUseForemanAgent: Boolean get() =
        entitlement?.hasFeature(SubscriptionFeature.ForemanAgent) == true || foremanAgentCredits > 0
}

class SubscriptionViewModel(
    observeUserEntitlementUseCase: ObserveUserEntitlementUseCase,
    private val refreshSubscriptionCatalogUseCase: RefreshSubscriptionCatalogUseCase,
    private val syncSubscriptionEntitlementUseCase: SyncSubscriptionEntitlementUseCase,
    private val getPaywallTiersUseCase: GetPaywallTiersUseCase,
    private val purchaseSubscriptionTierUseCase: PurchaseSubscriptionTierUseCase,
    private val restoreSubscriptionPurchasesUseCase: RestoreSubscriptionPurchasesUseCase,
    private val observeTokenBalancesUseCase: ObserveTokenBalancesUseCase,
    private val consumeTokenUseCase: ConsumeTokenUseCase,
    private val purchaseProductUseCase: PurchaseProductUseCase,
    private val reconcileTokensUseCase: ReconcileTokensUseCase
) : ViewModel() {

    val entitlement: StateFlow<UserEntitlement?> = observeUserEntitlementUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _uiState = MutableStateFlow(SubscriptionUiState())
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching {
                refreshSubscriptionCatalogUseCase()
                syncSubscriptionEntitlementUseCase()
                reconcileTokensUseCase()
            }
            _uiState.update { state ->
                state.copy(
                    tiers = getPaywallTiersUseCase(),
                    entitlement = entitlement.value
                )
            }
        }

        // Collect token balances flow reactive updates
        viewModelScope.launch {
            observeTokenBalancesUseCase().collect { balances ->
                _uiState.update { it.copy(tokenBalances = balances) }
            }
        }
    }

    fun refreshPaywall() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null, purchaseSuccess = false) }
            refreshSubscriptionCatalogUseCase()
            syncSubscriptionEntitlementUseCase()
            reconcileTokensUseCase()
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    tiers = getPaywallTiersUseCase(),
                    entitlement = entitlement.value
                )
            }
        }
    }

    fun purchase(tierId: SubscriptionTierId) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null, purchaseSuccess = false) }
            when (val outcome = purchaseSubscriptionTierUseCase(tierId)) {
                SubscriptionPurchaseOutcome.Cancelled ->
                    _uiState.update { it.copy(isLoading = false, message = "Purchase cancelled.") }
                is SubscriptionPurchaseOutcome.Failure ->
                    _uiState.update { it.copy(isLoading = false, message = outcome.error.value) }
                is SubscriptionPurchaseOutcome.Success ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            purchaseSuccess = true,
                            message = "Pro unlocked via ${outcome.entitlement.source.name}.",
                            entitlement = outcome.entitlement
                        )
                    }
            }
        }
    }

    fun purchaseProduct(product: PurchasableProduct) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null, purchaseSuccess = false) }
            when (val outcome = purchaseProductUseCase(product)) {
                BillingOutcome.Cancelled ->
                    _uiState.update { it.copy(isLoading = false, message = "Purchase cancelled.") }
                is BillingOutcome.Failure ->
                    _uiState.update { it.copy(isLoading = false, message = outcome.error.value) }
                is BillingOutcome.Success ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            purchaseSuccess = true,
                            message = "Purchased ${product.displayName}.",
                            entitlement = outcome.activeEntitlement
                        )
                    }
            }
        }
    }

    fun consumeToken(feature: PremiumFeature) {
        viewModelScope.launch {
            when (val outcome = consumeTokenUseCase(feature)) {
                is TokenConsumptionOutcome.Success -> {
                    // Token consumed successfully
                }
                is TokenConsumptionOutcome.InsufficientTokens -> {
                    _uiState.update { it.copy(message = "Low Balance! Deducting credits failed. Purchase more scans or Go Pro.") }
                }
                is TokenConsumptionOutcome.Failure -> {
                    _uiState.update { it.copy(message = outcome.error.value) }
                }
            }
        }
    }

    fun restore() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null, purchaseSuccess = false) }
            when (val outcome = restoreSubscriptionPurchasesUseCase()) {
                is SubscriptionRestoreOutcome.Failure ->
                    _uiState.update { it.copy(isLoading = false, message = outcome.error.value) }
                is SubscriptionRestoreOutcome.Success ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            purchaseSuccess = outcome.entitlement.isPro,
                            message = if (outcome.entitlement.isPro) {
                                "Purchases restored."
                            } else {
                                "No active subscription found."
                            },
                            entitlement = outcome.entitlement
                        )
                    }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, purchaseSuccess = false) }
    }
}
