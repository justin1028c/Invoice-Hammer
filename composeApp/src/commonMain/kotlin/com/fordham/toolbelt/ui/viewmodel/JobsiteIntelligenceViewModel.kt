package com.fordham.toolbelt.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.model.agent.DailyBriefing
import com.fordham.toolbelt.domain.usecase.agent.GetDailyBriefingOutcome
import com.fordham.toolbelt.domain.usecase.agent.GetDailyBriefingUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class JobsiteIntelligenceUiState(
    val isLoading: Boolean = false,
    val briefing: DailyBriefing? = null,
    val errorMessage: FailureMessage? = null
)

sealed interface JobsiteIntelligenceIntent {
    object RefreshBriefing : JobsiteIntelligenceIntent
}

class JobsiteIntelligenceViewModel(
    private val getDailyBriefingUseCase: GetDailyBriefingUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(JobsiteIntelligenceUiState())
    val uiState: StateFlow<JobsiteIntelligenceUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun onIntent(intent: JobsiteIntelligenceIntent) {
        when (intent) {
            is JobsiteIntelligenceIntent.RefreshBriefing -> refresh()
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val outcome = getDailyBriefingUseCase.execute()) {
                is GetDailyBriefingOutcome.Success -> {
                    _uiState.update { it.copy(isLoading = false, briefing = outcome.briefing) }
                }
                is GetDailyBriefingOutcome.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = outcome.message) }
                }
            }
        }
    }
}
