package com.fordham.toolbelt.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fordham.toolbelt.domain.model.stripe.StripeConnectOnboardingOutcome
import com.fordham.toolbelt.domain.model.stripe.StripeConnectSetupState
import com.fordham.toolbelt.domain.model.stripe.StripePaymentMode
import com.fordham.toolbelt.domain.repository.AuthRepository
import com.fordham.toolbelt.domain.usecase.stripe.GetStripePaymentModeUseCase
import com.fordham.toolbelt.domain.usecase.stripe.RefreshStripeConnectStatusUseCase
import com.fordham.toolbelt.domain.usecase.stripe.StartStripeConnectOnboardingUseCase
import com.fordham.toolbelt.util.UiMessageKeys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.fordham.toolbelt.data.local.LocalLlmEngine

class SettingsViewModel(
    getStripePaymentModeUseCase: GetStripePaymentModeUseCase,
    private val refreshStripeConnectStatusUseCase: RefreshStripeConnectStatusUseCase,
    private val startStripeConnectOnboardingUseCase: StartStripeConnectOnboardingUseCase,
    private val authRepository: AuthRepository,
    private val localLlmEngine: LocalLlmEngine
) : ViewModel() {
    val stripePaymentMode: StripePaymentMode = getStripePaymentModeUseCase()

    private val _connectState = MutableStateFlow<StripeConnectSetupState>(
        if (stripePaymentMode == StripePaymentMode.PaymentSheet) {
            StripeConnectSetupState.Loading
        } else {
            StripeConnectSetupState.BackendDisabled
        }
    )
    val connectState: StateFlow<StripeConnectSetupState> = _connectState.asStateFlow()

    private val _connectBusy = MutableStateFlow(false)
    val connectBusy: StateFlow<Boolean> = _connectBusy.asStateFlow()

    private val _isLlamaDownloaded = MutableStateFlow(localLlmEngine.isModelDownloaded())
    val isLlamaDownloaded: StateFlow<Boolean> = _isLlamaDownloaded.asStateFlow()

    private val _llamaDownloadProgress = MutableStateFlow(localLlmEngine.getDownloadProgress())
    val llamaDownloadProgress: StateFlow<Float> = _llamaDownloadProgress.asStateFlow()

    private val _isLlamaDownloading = MutableStateFlow(localLlmEngine.isDownloading())
    val isLlamaDownloading: StateFlow<Boolean> = _isLlamaDownloading.asStateFlow()

    init {
        if (stripePaymentMode == StripePaymentMode.PaymentSheet) {
            refreshConnectStatus()
        }
    }

    fun downloadLlama() {
        if (localLlmEngine.isDownloading()) return
        _isLlamaDownloading.value = true
        _llamaDownloadProgress.value = 0.0f
        localLlmEngine.startDownload(
            onProgress = { progress ->
                _llamaDownloadProgress.value = progress
            },
            onComplete = { success ->
                _isLlamaDownloading.value = false
                _isLlamaDownloaded.value = localLlmEngine.isModelDownloaded()
                _llamaDownloadProgress.value = if (success) 1.0f else 0.0f
            }
        )
    }

    fun deleteLlama() {
        localLlmEngine.deleteModel()
        _isLlamaDownloaded.value = false
        _llamaDownloadProgress.value = 0.0f
    }

    fun onSettingsTabVisible() {
        if (stripePaymentMode == StripePaymentMode.PaymentSheet &&
            authRepository.currentUser.value != null
        ) {
            refreshConnectStatus()
        }
    }

    fun refreshConnectStatus() {
        if (stripePaymentMode != StripePaymentMode.PaymentSheet) return
        viewModelScope.launch {
            _connectState.value = StripeConnectSetupState.Loading
            _connectState.value = refreshStripeConnectStatusUseCase()
        }
    }

    fun startConnectOnboarding(onOpenUrl: (String) -> Unit, onMessage: (String) -> Unit) {
        if (_connectBusy.value) return
        viewModelScope.launch {
            _connectBusy.value = true
            when (val outcome = startStripeConnectOnboardingUseCase()) {
                is StripeConnectOnboardingOutcome.Ready -> onOpenUrl(outcome.url.value)
                StripeConnectOnboardingOutcome.SignInRequired ->
                    onMessage(UiMessageKeys.STRIPE_SIGN_IN_REQUIRED)
                StripeConnectOnboardingOutcome.BackendNotConfigured ->
                    onMessage(UiMessageKeys.STRIPE_BACKEND_URL_REQUIRED)
                is StripeConnectOnboardingOutcome.Failure ->
                    onMessage(outcome.error.value)
            }
            _connectBusy.value = false
        }
    }
}
