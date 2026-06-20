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
import com.fordham.toolbelt.domain.model.JobNote
import com.fordham.toolbelt.domain.model.NoteId
import com.fordham.toolbelt.domain.model.ReceiptItem
import com.fordham.toolbelt.domain.model.ReceiptId
import com.fordham.toolbelt.domain.model.LineItem
import com.fordham.toolbelt.domain.model.Client
import com.fordham.toolbelt.domain.model.ClientId
import com.fordham.toolbelt.domain.repository.SettingsRepository
import com.fordham.toolbelt.domain.repository.InvoiceRepository
import com.fordham.toolbelt.domain.repository.ReceiptRepository
import com.fordham.toolbelt.domain.repository.JobNoteRepository
import com.fordham.toolbelt.domain.repository.ClientRepository
import com.fordham.toolbelt.domain.model.agent.SystemBudgetSerializer
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
    private val receiptRepository: ReceiptRepository,
    private val jobNoteRepository: JobNoteRepository,
    private val clientRepository: ClientRepository,
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
            
            // Seed standard clients
            clients.forEach { name ->
                clientRepository.insertClient(
                    Client(
                        id = ClientId(randomUUID()),
                        name = name,
                        email = EmailAddress("contact@${name.lowercase().replace(" ", "")}.com"),
                        phone = PhoneNumber("555-0100"),
                        address = "Industrial Park Rd"
                    )
                )
            }
            
            for (i in 1..1000) {
                val amt = (100..2000).random().toDouble()
                val desc = items[i % items.size]
                
                if (i % 10 == 0) {
                    // Profile/Matched Case for Jobsite Intelligence testing
                    val estimateId = InvoiceId(randomUUID())
                    val clientName = "Client IQ $i"
                    
                    // Seed scenario client
                    clientRepository.insertClient(
                        Client(
                            id = ClientId(randomUUID()),
                            name = clientName,
                            email = EmailAddress("stress@test.com"),
                            phone = PhoneNumber("555-0199"),
                            address = "123 Construction Rd"
                        )
                    )
                    
                    list.add(
                        Invoice(
                            id = estimateId,
                            clientName = clientName,
                            clientAddress = "123 Construction Rd",
                            clientPhone = PhoneNumber("555-0199"),
                            clientEmail = EmailAddress("stress@test.com"),
                            date = dateStr,
                            totalAmount = amt,
                            depositAmount = 0.0,
                            itemsSummary = desc,
                            pdfPath = "",
                            isPaid = false,
                            isEstimate = true,
                            lastUpdated = now - (i * 10000),
                            durationSeconds = (1800..28800).random().toLong()
                        )
                    )
                    
                    // Create [SYSTEM_BUDGET] note (30% of total invoice amount allocated as materials)
                    val budgetedMaterials = amt * 0.3
                    val lineItems = listOf(
                        LineItem("Materials Package", budgetedMaterials, "Materials")
                    )
                    val budgetNoteText = SystemBudgetSerializer.serialize(amt, budgetedMaterials, lineItems)
                    jobNoteRepository.insertNote(
                        JobNote(
                            id = NoteId(randomUUID()),
                            clientName = clientName,
                            text = budgetNoteText,
                            timestamp = now - (i * 10000) + 1000,
                            invoiceId = estimateId
                        )
                    )
                    
                    when {
                        i % 20 == 0 -> {
                            // Scenario Profile B: Material Overrun (25% overrun)
                            val actualMaterials = budgetedMaterials * 1.25
                            receiptRepository.insertItem(
                                ReceiptItem(
                                    id = ReceiptId(randomUUID()),
                                    description = "Lumber & Hardware Overrun",
                                    quantity = 1.0,
                                    unitPrice = actualMaterials,
                                    totalPrice = actualMaterials,
                                    clientName = clientName,
                                    isBilled = true,
                                    linkedInvoiceId = estimateId,
                                    lastUpdated = now - (i * 10000) + 2000
                                )
                            )
                            jobNoteRepository.insertNote(
                                JobNote(
                                    id = NoteId(randomUUID()),
                                    clientName = clientName,
                                    text = "Progress update: Material expenses monitored. No scope creep.",
                                    timestamp = now - (i * 10000) + 3000,
                                    invoiceId = estimateId
                                )
                            )
                        }
                        i % 30 == 0 -> {
                            // Scenario Profile C: Scope Creep / Change Order Opportunity
                            receiptRepository.insertItem(
                                ReceiptItem(
                                    id = ReceiptId(randomUUID()),
                                    description = "Lumber & Hardware",
                                    quantity = 1.0,
                                    unitPrice = budgetedMaterials,
                                    totalPrice = budgetedMaterials,
                                    clientName = clientName,
                                    isBilled = true,
                                    linkedInvoiceId = estimateId,
                                    lastUpdated = now - (i * 10000) + 2000
                                )
                            )
                            jobNoteRepository.insertNote(
                                JobNote(
                                    id = NoteId(randomUUID()),
                                    clientName = clientName,
                                    text = "Owner asked to patch two extra walls in the hallway. Also added a custom dimmer switch.",
                                    timestamp = now - (i * 10000) + 3000,
                                    invoiceId = estimateId
                                )
                            )
                        }
                        else -> {
                            // Scenario Profile A: Perfect Profit / Budget Match
                            receiptRepository.insertItem(
                                ReceiptItem(
                                    id = ReceiptId(randomUUID()),
                                    description = "Lumber & Hardware",
                                    quantity = 1.0,
                                    unitPrice = budgetedMaterials,
                                    totalPrice = budgetedMaterials,
                                    clientName = clientName,
                                    isBilled = true,
                                    linkedInvoiceId = estimateId,
                                    lastUpdated = now - (i * 10000) + 2000
                                )
                            )
                            jobNoteRepository.insertNote(
                                JobNote(
                                    id = NoteId(randomUUID()),
                                    clientName = clientName,
                                    text = "Standard install completed. No unexpected work requested.",
                                    timestamp = now - (i * 10000) + 3000,
                                    invoiceId = estimateId
                                )
                            )
                        }
                    }
                } else {
                    // Standard Invoice simulation
                    val client = clients[i % clients.size]
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
            }
            invoiceRepository.insertInvoices(list)
        }
    }
 
    fun eraseAllInvoices() {
        viewModelScope.launch(ioDispatcher) {
            invoiceRepository.deleteAllInvoices()
            receiptRepository.deleteAllItems()
            jobNoteRepository.deleteAllNotes()
            clientRepository.replaceAllClients(emptyList())
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
