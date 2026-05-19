package com.fordham.toolbelt.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fordham.toolbelt.domain.model.SyncOutcome
import com.fordham.toolbelt.domain.repository.AuthRepository
import com.fordham.toolbelt.domain.repository.FordhamUser
import com.fordham.toolbelt.domain.repository.SyncRepository
import com.fordham.toolbelt.domain.repository.IdToken
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {

    val currentUser: StateFlow<FordhamUser?> = authRepository.currentUser
    
    private val _syncState = kotlinx.coroutines.flow.MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    fun signIn(idToken: String) {
        viewModelScope.launch {
            authRepository.signInWithGoogle(IdToken(idToken))
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    fun triggerBackup() {
        viewModelScope.launch {
            _syncState.value = SyncState.Syncing
            val result = syncRepository.syncInvoices()
            when (result) {
                is SyncOutcome.Success -> {
                    _syncState.value = SyncState.Success
                    // Reset to idle after a delay so the UI can show success briefly
                    kotlinx.coroutines.delay(3000)
                    _syncState.value = SyncState.Idle
                }
                is SyncOutcome.Failure -> {
                    _syncState.value = SyncState.Error(result.error.value)
                }
            }
        }
    }

    fun clearSyncState() {
        _syncState.value = SyncState.Idle
    }
}

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    object Success : SyncState()
    data class Error(val message: String) : SyncState()
}
