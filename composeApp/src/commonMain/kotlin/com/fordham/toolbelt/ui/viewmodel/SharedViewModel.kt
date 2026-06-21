package com.fordham.toolbelt.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.repository.*
import com.fordham.toolbelt.domain.usecase.GetClientFinancialSummaryUseCase
import com.fordham.toolbelt.domain.usecase.SaveBusinessLogoUseCase
import com.fordham.toolbelt.domain.usecase.SyncUnpaidInvoiceRemindersUseCase
import com.fordham.toolbelt.util.UiMessageKeys
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SharedViewModel(
    private val settingsRepository: SettingsRepository,
    private val clientRepository: ClientRepository,
    private val jobNoteRepository: JobNoteRepository,
    private val photoRepository: PhotoRepository,
    private val invoiceRepository: InvoiceRepository,
    private val receiptRepository: ReceiptRepository,
    private val getClientFinancialSummaryUseCase: GetClientFinancialSummaryUseCase,
    private val saveBusinessLogoUseCase: SaveBusinessLogoUseCase,
    private val syncUnpaidInvoiceRemindersUseCase: SyncUnpaidInvoiceRemindersUseCase
) : ViewModel() {

    private val _logoMessage = MutableSharedFlow<String>()
    val logoMessage = _logoMessage.asSharedFlow()

    private val _selectedClient = MutableStateFlow<Client?>(null)
    val selectedClient: StateFlow<Client?> = _selectedClient.asStateFlow()

    val businessSettings: Flow<BusinessSettings> = settingsRepository.businessSettingsFlow
    val allClients: Flow<List<Client>> = clientRepository.getAllClients().map { result ->
        if (result is ClientListOutcome.Success) {
            result.clients
        } else emptyList()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedClientSummary: StateFlow<com.fordham.toolbelt.domain.usecase.FinancialSummary?> = _selectedClient.flatMapLatest { client ->
        if (client == null) flowOf(null)
        else getClientFinancialSummaryUseCase(client.name.value)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedClientInvoices: StateFlow<List<Invoice>> = _selectedClient.flatMapLatest { client ->
        if (client == null) flowOf(emptyList<Invoice>())
        else invoiceRepository.allInvoices.map { list -> 
            list.filter { it.clientName.value.trim().equals(client.name.value.trim(), ignoreCase = true) } 
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedClientPhotos: StateFlow<List<JobPhoto>> = _selectedClient.flatMapLatest { client ->
        if (client == null) flowOf(emptyList())
        else invoiceRepository.allInvoices.flatMapLatest { invoices ->
            val clientInvoiceIds = invoices.filter { it.clientName.value.trim().equals(client.name.value.trim(), ignoreCase = true) }.map { it.id }
            if (clientInvoiceIds.isEmpty()) flowOf(emptyList())
            else {
                val flows = clientInvoiceIds.map { photoRepository.observePhotosForInvoice(it) }
                combine(flows) { array: Array<*> -> 
                    array.flatMap { (it as? List<*>).orEmpty().filterIsInstance<JobPhoto>() }.sortedByDescending { p -> p.timestamp } 
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())




    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedClientNotes: StateFlow<List<JobNote>> = _selectedClient.flatMapLatest { client ->
        if (client == null) flowOf(emptyList<JobNote>())
        else jobNoteRepository.getNotesByClient(client.name.value)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectClient(client: Client?) {
        _selectedClient.value = client
    }

    fun selectClientByName(name: String) {
        viewModelScope.launch {
            val clients = allClients.first()
            val match = clients.find { it.name.value.equals(name, ignoreCase = true) }
            if (match != null) {
                _selectedClient.value = match
            }
        }
    }

    fun selectClientById(clientId: com.fordham.toolbelt.domain.model.ClientId) {
        viewModelScope.launch {
            val match = allClients.first().find { it.id == clientId }
            if (match != null) {
                _selectedClient.value = match
            }
        }
    }

    fun saveBusinessSettings(settings: BusinessSettings) {
        viewModelScope.launch {
            settingsRepository.saveBusinessSettings(settings)
            syncUnpaidInvoiceRemindersUseCase.execute()
        }
    }

    fun saveBusinessLogo(pickedUri: String?) {
        viewModelScope.launch {
            val message = when (val outcome = saveBusinessLogoUseCase(pickedUri)) {
                is SaveBusinessLogoOutcome.Saved -> UiMessageKeys.BUSINESS_LOGO_SAVED
                is SaveBusinessLogoOutcome.Cleared -> UiMessageKeys.BUSINESS_LOGO_REMOVED
                is SaveBusinessLogoOutcome.Failure -> outcome.error.value
            }
            _logoMessage.emit(message)
        }
    }
}
