package com.fordham.toolbelt.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fordham.toolbelt.domain.model.BusinessSettings
import com.fordham.toolbelt.domain.model.BusinessStats
import com.fordham.toolbelt.domain.model.TaxExportOutcome
import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.InvoiceId
import com.fordham.toolbelt.domain.model.PhoneNumber
import com.fordham.toolbelt.domain.model.EmailAddress
import com.fordham.toolbelt.domain.repository.SettingsRepository
import com.fordham.toolbelt.domain.repository.InvoiceRepository
import com.fordham.toolbelt.domain.usecase.GenerateTaxReportUseCase
import com.fordham.toolbelt.domain.usecase.GetBusinessStatsUseCase
import com.fordham.toolbelt.util.DateTimeUtil
import com.fordham.toolbelt.util.randomUUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StatsViewModel(
    private val getBusinessStatsUseCase: GetBusinessStatsUseCase,
    private val generateTaxReportUseCase: GenerateTaxReportUseCase,
    private val settingsRepository: SettingsRepository,
    private val invoiceRepository: InvoiceRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val businessStats: StateFlow<BusinessStats> = getBusinessStatsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BusinessStats())

    val businessSettings: StateFlow<BusinessSettings> = settingsRepository.businessSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BusinessSettings())

    fun togglePremium() {
        viewModelScope.launch {
            val currentSettings = businessSettings.value
            settingsRepository.saveBusinessSettings(
                currentSettings.copy(isPremium = !currentSettings.isPremium)
            )
        }
    }

    fun exportBentoReport(onGenerated: (String, String?) -> Unit) {
        viewModelScope.launch(ioDispatcher) {
            when (val outcome = generateTaxReportUseCase.executeBentoReport()) {
                is TaxExportOutcome.Success -> {
                    withContext(mainDispatcher) {
                        onGenerated(outcome.path, outcome.savedTo)
                    }
                }
                is TaxExportOutcome.Failure -> {
                    _errorMessage.value = outcome.error.value
                }
                TaxExportOutcome.Loading -> {
                    // No-op or handle loading state if required in future UI upgrades
                }
            }
        }
    }

    fun exportTaxBundle(onGenerated: (String, String?) -> Unit) {
        viewModelScope.launch(ioDispatcher) {
            when (val outcome = generateTaxReportUseCase.executeZip()) {
                is TaxExportOutcome.Success -> {
                    withContext(mainDispatcher) {
                        onGenerated(outcome.path, outcome.savedTo)
                    }
                }
                is TaxExportOutcome.Failure -> {
                    _errorMessage.value = outcome.error.value
                }
                TaxExportOutcome.Loading -> {
                    // No-op or handle loading state if required in future UI upgrades
                }
            }
        }
    }

    fun createStressTestInvoices() {
        viewModelScope.launch(ioDispatcher) {
            val list = mutableListOf<Invoice>()
            val now = DateTimeUtil.nowEpochMillis()
            val dateStr = "2026-05-17"
            val clients = listOf("Alpha Builders", "Omega Plumbing", "Ironwood Carpentry", "Summit HVAC", "Evergreen Landscapes")
            val items = listOf("Foundations Construction", "Pipe Repairs", "Frame Carpentry", "System Installation", "Sod Layout")
            
            for (i in 1..1000) {
                val client = clients[i % clients.size]
                val desc = items[i % items.size]
                val amt = (100..2000).random().toDouble()
                val isPaid = i % 2 == 0
                val isEstimate = i % 5 != 0
                
                list.add(
                    Invoice(
                        id = InvoiceId(randomUUID()),
                        clientName = client,
                        clientAddress = "123 Construction Rd",
                        clientPhone = PhoneNumber("555-0199"),
                        clientEmail = EmailAddress("stress@test.com"),
                        date = dateStr,
                        totalAmount = amt,
                        depositAmount = if (isPaid) amt * 0.1 else 0.0,
                        itemsSummary = desc,
                        pdfPath = "",
                        isPaid = isPaid,
                        isEstimate = isEstimate,
                        lastUpdated = now - (i * 10000),
                        durationSeconds = (1800..28800).random().toLong()
                    )
                )
            }
            invoiceRepository.insertInvoices(list)
        }
    }

    fun eraseAllInvoices() {
        viewModelScope.launch(ioDispatcher) {
            invoiceRepository.deleteAllInvoices()
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
