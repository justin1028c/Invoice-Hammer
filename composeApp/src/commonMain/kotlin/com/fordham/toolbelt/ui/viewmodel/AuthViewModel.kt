package com.fordham.toolbelt.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fordham.toolbelt.domain.model.SupabaseConnectionMode
import com.fordham.toolbelt.domain.model.SyncOutcome
import com.fordham.toolbelt.domain.repository.AuthOutcome
import com.fordham.toolbelt.domain.repository.AuthRepository
import com.fordham.toolbelt.domain.repository.FordhamUser
import com.fordham.toolbelt.domain.repository.SyncRepository
import com.fordham.toolbelt.domain.repository.IdToken
import com.fordham.toolbelt.domain.usecase.GetSupabaseConnectionModeUseCase
import com.fordham.toolbelt.domain.usecase.subscription.RefreshSubscriptionCatalogUseCase
import com.fordham.toolbelt.domain.usecase.subscription.SyncSubscriptionEntitlementUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val syncRepository: SyncRepository,
    getSupabaseConnectionModeUseCase: GetSupabaseConnectionModeUseCase,
    private val refreshSubscriptionCatalogUseCase: RefreshSubscriptionCatalogUseCase,
    private val syncSubscriptionEntitlementUseCase: SyncSubscriptionEntitlementUseCase
) : ViewModel() {

    val currentUser: StateFlow<FordhamUser?> = authRepository.currentUser
    val supabaseConnectionMode: SupabaseConnectionMode = getSupabaseConnectionModeUseCase()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _authMessage = MutableStateFlow<String?>(null)
    val authMessage: StateFlow<String?> = _authMessage.asStateFlow()

    init {
        viewModelScope.launch {
            if (authRepository.currentUser.value != null) {
                refreshSubscriptionStateForCurrentSession()
            }
        }
    }

    fun signIn(idToken: String) {
        viewModelScope.launch {
            when (val outcome = authRepository.signInWithGoogle(IdToken(idToken))) {
                is AuthOutcome.Authenticated -> {
                    _authMessage.value = null
                    refreshSubscriptionStateForCurrentSession()
                }
                is AuthOutcome.Failure -> {
                    _authMessage.value = outcome.error.value
                }
                AuthOutcome.SignedOut -> Unit
            }
        }
    }

    fun clearAuthMessage() {
        _authMessage.value = null
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    fun triggerBackup() {
        viewModelScope.launch {
            _syncState.value = SyncState.Syncing(CloudSyncOperation.Backup)
            handleSyncResult(syncRepository.syncInvoices(), CloudSyncOperation.Backup)
        }
    }

    fun triggerRestore() {
        viewModelScope.launch {
            _syncState.value = SyncState.Syncing(CloudSyncOperation.Restore)
            handleSyncResult(syncRepository.restoreLatest(), CloudSyncOperation.Restore)
        }
    }

    fun clearSyncState() {
        _syncState.value = SyncState.Idle
    }

    private suspend fun refreshSubscriptionStateForCurrentSession() {
        refreshSubscriptionCatalogUseCase()
        syncSubscriptionEntitlementUseCase()
        authRepository.refreshCurrentUserPremiumStatus()
    }

    private suspend fun handleSyncResult(result: SyncOutcome, operation: CloudSyncOperation) {
        when (result) {
            is SyncOutcome.Success -> {
                _syncState.value = SyncState.Success(operation)
                delay(3000)
                _syncState.value = SyncState.Idle
            }
            is SyncOutcome.Failure -> {
                _syncState.value = SyncState.Error(result.error.value, operation)
            }
        }
    }
}

enum class CloudSyncOperation {
    Backup,
    Restore
}

sealed class SyncState {
    data object Idle : SyncState()
    data class Syncing(val operation: CloudSyncOperation) : SyncState()
    data class Success(val operation: CloudSyncOperation) : SyncState()
    data class Error(val message: String, val operation: CloudSyncOperation) : SyncState()
}
