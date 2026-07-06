package com.fordham.toolbelt.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fordham.toolbelt.domain.model.*
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
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

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
            val now = DateTimeUtil.nowEpochMillis()
            val fixtures = listOf(
                DemoInvoiceFixture(
                    clientName = "Eleanor Brooks",
                    address = "1189 West Briar Court, Macon, GA 31210",
                    phone = "478-555-0142",
                    email = "eleanor.brooks@example.com",
                    daysAgo = 1,
                    total = 548.75,
                    deposit = 50.0,
                    summary = "Installed 42 linear feet of baseboard, painted hallway trim, repaired 2 nail pops",
                    isPaid = false,
                    isEstimate = false,
                    durationSeconds = 4_800L,
                    materialCost = 146.32,
                    note = "Customer asked for satin white trim finish. Hallway nail pops were repaired before paint touch-up."
                ),
                DemoInvoiceFixture(
                    clientName = "Nadia Coleman",
                    address = "506 Lakeview Drive, Augusta, GA 30907",
                    phone = "706-555-0188",
                    email = "nadia.coleman@example.com",
                    daysAgo = 4,
                    total = 410.40,
                    deposit = 0.0,
                    summary = "Replaced garbage disposal, installed shut off valve, hauled away old unit",
                    isPaid = true,
                    isEstimate = false,
                    durationSeconds = 3_300L,
                    materialCost = 188.64,
                    note = "Kitchen sink tested with no leaks after disposal replacement and valve install."
                ),
                DemoInvoiceFixture(
                    clientName = "Marcus Hill",
                    address = "742 Pine Ridge Lane, Rex, GA 30273",
                    phone = "770-555-0164",
                    email = "marcus.hill@example.com",
                    daysAgo = 9,
                    total = 1_286.00,
                    deposit = 250.0,
                    summary = "Replaced damaged deck boards, reset handrail posts, pressure washed landing",
                    isPaid = false,
                    isEstimate = false,
                    durationSeconds = 9_900L,
                    materialCost = 462.18,
                    note = "Two extra boards near the stairs were soft and should be monitored after the next rain."
                ),
                DemoInvoiceFixture(
                    clientName = "Scott Fordham",
                    address = "3132 Brookhollow Drive, Rex, GA 30238",
                    phone = "678-555-0129",
                    email = "scott.fordham@example.com",
                    daysAgo = 14,
                    total = 875.00,
                    deposit = 0.0,
                    summary = "Patched drywall opening, skim coated wall, sanded and primed repair area",
                    isPaid = false,
                    isEstimate = true,
                    durationSeconds = 0L,
                    materialCost = 96.41,
                    note = "Estimate includes drywall repair and primer only. Paint match may be billed separately."
                ),
                DemoInvoiceFixture(
                    clientName = "Angela Ramirez",
                    address = "2298 Oak Terrace, Jonesboro, GA 30236",
                    phone = "404-555-0193",
                    email = "angela.ramirez@example.com",
                    daysAgo = 22,
                    total = 735.80,
                    deposit = 100.0,
                    summary = "Installed 3 GFCI outlets, replaced porch light fixture, labeled breaker panel",
                    isPaid = true,
                    isEstimate = false,
                    durationSeconds = 5_700L,
                    materialCost = 214.08,
                    note = "Exterior fixture was replaced with weather-rated unit. GFCI outlets passed test/reset check."
                )
            )

            val invoices = fixtures.mapIndexed { index, fixture ->
                val invoiceId = InvoiceId(randomUUID())
                clientRepository.insertClient(
                    Client(
                        id = ClientId(randomUUID()),
                        name = ClientName(fixture.clientName),
                        email = EmailAddress(fixture.email),
                        phone = PhoneNumber(fixture.phone),
                        address = ClientAddress(fixture.address)
                    )
                )
                receiptRepository.insertItem(
                    ReceiptItem(
                        id = ReceiptId(randomUUID()),
                        description = "${fixture.summary.substringBefore(",")} materials",
                        quantity = 1.0,
                        unitPrice = fixture.materialCost,
                        totalPrice = fixture.materialCost,
                        clientName = fixture.clientName,
                        isBilled = true,
                        linkedInvoiceId = invoiceId,
                        lastUpdated = now - fixture.daysAgo.daysToMillis() + 2_000L
                    )
                )
                jobNoteRepository.insertNote(
                    JobNote(
                        id = NoteId(randomUUID()),
                        clientName = fixture.clientName,
                        text = fixture.note,
                        timestamp = now - fixture.daysAgo.daysToMillis() + 3_000L,
                        invoiceId = invoiceId
                    )
                )
                val budgetedMaterials = fixture.total * 0.35
                jobNoteRepository.insertNote(
                    JobNote(
                        id = NoteId(randomUUID()),
                        clientName = fixture.clientName,
                        text = SystemBudgetSerializer.serialize(
                            fixture.total,
                            budgetedMaterials,
                            listOf(LineItem(ItemsSummary("Budgeted materials"), MoneyAmount(budgetedMaterials), "Materials"))
                        ),
                        timestamp = now - fixture.daysAgo.daysToMillis() + 1_000L,
                        invoiceId = invoiceId
                    )
                )
                Invoice(
                    id = invoiceId,
                    clientName = ClientName(fixture.clientName),
                    clientAddress = ClientAddress(fixture.address),
                    clientPhone = PhoneNumber(fixture.phone),
                    clientEmail = EmailAddress(fixture.email),
                    date = dateDaysAgo(fixture.daysAgo),
                    totalAmount = MoneyAmount(fixture.total),
                    depositAmount = MoneyAmount(fixture.deposit),
                    itemsSummary = ItemsSummary(fixture.summary),
                    pdfPath = PdfFilePath(""),
                    isPaid = fixture.isPaid,
                    isEstimate = fixture.isEstimate,
                    lastUpdated = now - fixture.daysAgo.daysToMillis() + index,
                    durationSeconds = DurationSeconds(fixture.durationSeconds)
                )
            }
            invoiceRepository.insertInvoices(invoices)
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

    private fun dateDaysAgo(daysAgo: Int): String {
        val millis = DateTimeUtil.nowEpochMillis() - daysAgo.daysToMillis()
        val dt = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())
        val month = dt.monthNumber.toString().padStart(2, '0')
        val day = dt.dayOfMonth.toString().padStart(2, '0')
        return "${dt.year}-$month-$day"
    }

    private fun Int.daysToMillis(): Long = this * 24L * 60L * 60L * 1000L

    private data class DemoInvoiceFixture(
        val clientName: String,
        val address: String,
        val phone: String,
        val email: String,
        val daysAgo: Int,
        val total: Double,
        val deposit: Double,
        val summary: String,
        val isPaid: Boolean,
        val isEstimate: Boolean,
        val durationSeconds: Long,
        val materialCost: Double,
        val note: String
    )
}
