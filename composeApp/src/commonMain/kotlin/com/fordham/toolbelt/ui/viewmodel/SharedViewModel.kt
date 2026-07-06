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

    private val _selectedClientId = MutableStateFlow<ClientId?>(null)
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedClient: StateFlow<Client?> = _selectedClientId.flatMapLatest { id ->
        if (id == null) flowOf(null)
        else clientRepository.getAllClients().map { outcome ->
            if (outcome is ClientListOutcome.Success) {
                outcome.clients.find { it.id == id }
            } else null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val businessSettings: Flow<BusinessSettings> = settingsRepository.businessSettingsFlow
    val allClients: Flow<List<Client>> = clientRepository.getAllClients().map { result ->
        if (result is ClientListOutcome.Success) {
            result.clients
        } else emptyList()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedClientSummary: StateFlow<com.fordham.toolbelt.domain.usecase.FinancialSummary?> = selectedClient.flatMapLatest { client ->
        if (client == null) flowOf(null)
        else getClientFinancialSummaryUseCase(client.name.value)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedClientInvoices: StateFlow<List<Invoice>> = selectedClient.flatMapLatest { client ->
        if (client == null) flowOf(emptyList<Invoice>())
        else invoiceRepository.allInvoices.map { list -> 
            list.filter { it.clientName.value.trim().equals(client.name.value.trim(), ignoreCase = true) } 
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedClientPhotos: StateFlow<List<JobPhoto>> = selectedClient.flatMapLatest { client ->
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
    val selectedClientNotes: StateFlow<List<JobNote>> = selectedClient.flatMapLatest { client ->
        if (client == null) flowOf(emptyList<JobNote>())
        else jobNoteRepository.getNotesByClient(client.name.value)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectClient(client: Client?) {
        _selectedClientId.value = client?.id
    }

    fun selectClientByName(name: String) {
        viewModelScope.launch {
            val clients = allClients.first()
            val match = clients.find { it.name.value.equals(name, ignoreCase = true) }
            if (match != null) {
                _selectedClientId.value = match.id
            }
        }
    }

    fun selectClientById(clientId: com.fordham.toolbelt.domain.model.ClientId) {
        _selectedClientId.value = clientId
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
