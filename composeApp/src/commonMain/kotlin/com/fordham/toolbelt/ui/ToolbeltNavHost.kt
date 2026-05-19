package com.fordham.toolbelt.ui

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.ui.tabs.*
import com.fordham.toolbelt.ui.tabs.suppliers.SupplierUiModel
import com.fordham.toolbelt.ui.viewmodel.*
import com.fordham.toolbelt.util.PlatformActions
import com.fordham.toolbelt.util.Permission

@Composable
fun ToolbeltNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    newInvoiceViewModel: NewInvoiceViewModel,
    sharedViewModel: SharedViewModel,
    clientsViewModel: ClientsViewModel,
    historyViewModel: HistoryViewModel,
    receiptsViewModel: ReceiptsViewModel,
    statsViewModel: StatsViewModel,
    suppliersViewModel: SuppliersViewModel,
    authViewModel: AuthViewModel,
    platformActions: PlatformActions
) {
    NavHost(
        navController = navController,
        startDestination = Screen.NewInvoice,
        modifier = modifier
    ) {
        composable<Screen.NewInvoice> {
            val uiState by newInvoiceViewModel.uiState.collectAsStateWithLifecycle()
            val businessSettings by sharedViewModel.businessSettings.collectAsStateWithLifecycle(initialValue = BusinessSettings())
            val allClients by clientsViewModel.allClients.collectAsStateWithLifecycle(initialValue = emptyList())

            NewInvoiceTab(
                uiState = uiState,
                businessSettings = businessSettings,
                allClients = allClients,
                categories = newInvoiceViewModel.categories,
                onSaveBusinessSettings = { sharedViewModel.saveBusinessSettings(it) },
                onTimerToggle = { newInvoiceViewModel.onIntent(NewInvoiceIntent.ToggleTimer) },
                onHourlyRateChange = { newInvoiceViewModel.onIntent(NewInvoiceIntent.OnHourlyRateChange(it)) },
                onBillLabor = { newInvoiceViewModel.onIntent(NewInvoiceIntent.BillLabor) },
                onLogoUriChange = { newInvoiceViewModel.onIntent(NewInvoiceIntent.OnLogoUriChange(it)) },
                onPhotoCaptured = { newInvoiceViewModel.onIntent(NewInvoiceIntent.OnPhotoCaptured(it)) },
                onClientNameChange = { newInvoiceViewModel.onIntent(NewInvoiceIntent.OnClientNameChange(it)) },
                onClientAddressChange = { newInvoiceViewModel.onIntent(NewInvoiceIntent.OnClientAddressChange(it)) },
                onSetInvoiceClientDropdownVisible = { newInvoiceViewModel.onIntent(NewInvoiceIntent.SetClientDropdownVisible(it)) },
                onSaveToClientDirectoryChange = { newInvoiceViewModel.onIntent(NewInvoiceIntent.OnSaveToClientDirectoryChange(it)) },
                onRemovePhoto = { newInvoiceViewModel.onIntent(NewInvoiceIntent.RemovePhoto(it)) },
                onSetReceiptPickerVisible = { newInvoiceViewModel.onIntent(NewInvoiceIntent.SetReceiptPickerVisible(it)) },
                onSetInvoiceCategoryDropdownVisible = { newInvoiceViewModel.onIntent(NewInvoiceIntent.SetCategoryDropdownVisible(it)) },
                onCategoryChange = { newInvoiceViewModel.onIntent(NewInvoiceIntent.OnCategoryChange(it)) },
                onItemDescChange = { newInvoiceViewModel.onIntent(NewInvoiceIntent.OnItemDescChange(it)) },
                onItemAmtChange = { newInvoiceViewModel.onIntent(NewInvoiceIntent.OnItemAmtChange(it)) },
                onProcessInvoiceAi = { newInvoiceViewModel.onIntent(NewInvoiceIntent.ProcessInvoiceAi(it)) },
                onAddManualLineItem = { newInvoiceViewModel.onIntent(NewInvoiceIntent.AddManualLineItem) },
                onRemoveLineItem = { newInvoiceViewModel.onIntent(NewInvoiceIntent.RemoveLineItem(it)) },
                onTaxTextChange = { newInvoiceViewModel.onIntent(NewInvoiceIntent.OnTaxTextChange(it)) },
                onDepositCollectedChange = { newInvoiceViewModel.onIntent(NewInvoiceIntent.OnDepositCollectedChange(it)) },
                onSaveInvoice = { isEstimate, settings, onComplete ->
                    newInvoiceViewModel.onIntent(NewInvoiceIntent.SaveInvoice(isEstimate, settings, onComplete))
                },
                onLinkReceipt = { receipt, markup -> newInvoiceViewModel.onIntent(NewInvoiceIntent.LinkReceipt(receipt, markup)) },
                onShareFile = { file, title -> platformActions.shareFile(file, title) },
                isPremium = businessSettings.isPremium,
                platformActions = platformActions
            )
        }
        composable<Screen.History> {
            val uiState by historyViewModel.uiState.collectAsStateWithLifecycle()
            val filteredHistory by historyViewModel.filteredInvoices.collectAsStateWithLifecycle(initialValue = emptyList())

            HistoryTab(
                uiState = uiState,
                filteredHistory = filteredHistory,
                paymentRequests = emptyList(),
                onViewPdf = { file -> platformActions.openPdf(file) },
                onSharePdf = { file, title -> platformActions.shareFile(file, title) },
                onRequestDeposit = { platformActions.showToast("Open this screen from the main shell to use mock Stellar payments.") },
                onRequestFullPayment = { platformActions.showToast("Open this screen from the main shell to use mock Stellar payments.") },
                onSetInvoiceToDelete = { historyViewModel.setInvoiceToDelete(it) },
                onDeleteInvoice = { historyViewModel.deleteInvoice(it) },
                onSearchQueryChange = { historyViewModel.onSearchQueryChange(it) },
                onShowPaidOnlyChange = { historyViewModel.onShowPaidOnlyChange(it) },
                onUpdateInvoice = { historyViewModel.updateInvoice(it) },
                onConvertEstimateToInvoice = { historyViewModel.convertEstimateToInvoice(it) },
                platformActions = platformActions
            )
        }
        composable<Screen.Receipts> {
            val uiState by receiptsViewModel.uiState.collectAsStateWithLifecycle()
            val selectedClient by sharedViewModel.selectedClient.collectAsStateWithLifecycle()
            val allClients by sharedViewModel.allClients.collectAsStateWithLifecycle(initialValue = emptyList())
            val filteredReceipts by receiptsViewModel.filteredReceipts.collectAsStateWithLifecycle(initialValue = emptyList())
            val receiptsTotal by receiptsViewModel.receiptsTotal.collectAsStateWithLifecycle(initialValue = 0.0)
            val totalWithMarkup by receiptsViewModel.totalWithMarkup.collectAsStateWithLifecycle(initialValue = 0.0)

            ReceiptsTab(
                uiState = uiState,
                selectedClient = selectedClient,
                allClients = allClients,
                filteredReceipts = filteredReceipts,
                receiptsTotal = receiptsTotal,
                totalWithMarkup = totalWithMarkup,
                onSetFilterClient = { receiptsViewModel.setFilterClient(it) },
                onSetClearConfirmVisible = { receiptsViewModel.setClearConfirmVisible(it) },
                onClearReceiptItems = { receiptsViewModel.clearReceiptItems() },
                onReceiptUriSelected = { receiptsViewModel.onReceiptUriSelected(it) },
                onSetClientDropdownVisible = { receiptsViewModel.setClientDropdownVisible(it) },
                onSelectClient = { sharedViewModel.selectClient(it) },
                onSetMarkupDialogVisible = { receiptsViewModel.setMarkupDialogVisible(it) },
                onMarkupPercentageChange = { receiptsViewModel.onMarkupPercentageChange(it) },
                onProcessReceipt = { receiptsViewModel.processCapturedReceipt(selectedClient) {} },
                onClearCapturedReceipt = { receiptsViewModel.clearCapturedReceiptImage() },
                onToggleReceiptBilled = { receiptsViewModel.toggleReceiptBilled(it) },
                onDeleteReceiptItem = { receiptsViewModel.deleteReceiptItem(it) },
                platformActions = platformActions
            )
        }
        composable<Screen.Stats> {
            val stats by statsViewModel.businessStats.collectAsStateWithLifecycle(initialValue = BusinessStats())
            val settings by statsViewModel.businessSettings.collectAsStateWithLifecycle()

            StatsTab(
                stats = stats,
                settings = settings,
                onExportCsv = {
                    statsViewModel.exportBentoReport { file: String ->
                        platformActions.shareFile(file, "Tax Report")
                    }
                },
                onExportZip = {
                    statsViewModel.exportTaxBundle { file: String ->
                        platformActions.shareFile(file, "Tax Bundle")
                    }
                },
                onNavigateToSettings = { navController.navigate(Screen.Settings) },
                onInsertStressInvoices = { statsViewModel.createStressTestInvoices() },
                onEraseAllInvoices = { statsViewModel.eraseAllInvoices() }
            )
        }
        composable<Screen.Clients> {
            val clients by clientsViewModel.allClients.collectAsStateWithLifecycle(initialValue = emptyList())
            val selectedClient by sharedViewModel.selectedClient.collectAsStateWithLifecycle()
            val clientInvoices by sharedViewModel.selectedClientInvoices.collectAsStateWithLifecycle()
            val summary by sharedViewModel.selectedClientSummary.collectAsStateWithLifecycle()
            val jobNotes by sharedViewModel.selectedClientNotes.collectAsStateWithLifecycle()
            val jobPhotos by sharedViewModel.selectedClientPhotos.collectAsStateWithLifecycle()
            val uiState by clientsViewModel.uiState.collectAsStateWithLifecycle()

            ClientsTab(
                clients = clients,
                selectedClient = selectedClient,
                clientInvoices = clientInvoices,
                summary = summary,
                jobNotes = jobNotes,
                jobPhotos = jobPhotos,
                uiState = uiState,
                onClientClick = { sharedViewModel.selectClient(it) },
                onDeleteClient = { clientsViewModel.onIntent(ClientsIntent.DeleteClient(it)) },
                onSetClientToDelete = { clientsViewModel.onIntent(ClientsIntent.SetClientToDelete(it)) },
                onBackClick = { sharedViewModel.selectClient(null) },
                onAddNote = { clientsViewModel.onIntent(ClientsIntent.AddNote(it)) },
                onDeleteNote = { clientsViewModel.onIntent(ClientsIntent.DeleteNote(it)) },
                onSummarizeNotes = { clientsViewModel.onIntent(ClientsIntent.SummarizeNotes(jobNotes)) },
                onLinkReceipt = { receipt -> 
                    selectedClient?.let { clientsViewModel.onIntent(ClientsIntent.LinkReceipt(receipt, it.name)) }
                },
                onViewPdf = { file -> platformActions.openPdf(file) },
                onSetNoteText = { clientsViewModel.onIntent(ClientsIntent.OnNoteTextChange(it)) },
                onSetAddNoteVisible = { clientsViewModel.onIntent(ClientsIntent.SetAddNoteVisible(it)) },
                onSetReceiptPickerVisible = { clientsViewModel.onIntent(ClientsIntent.SetReceiptPickerVisible(it)) },
                onClearAiSummary = { clientsViewModel.onIntent(ClientsIntent.ClearAiSummary) },
                onCallClient = { phone -> 
                    platformActions.callPhone(phone)
                },
                onEmailClient = { email ->
                    platformActions.sendEmail(email)
                },
                onPhotoCaptured = { uri, invId ->
                    clientsViewModel.onIntent(ClientsIntent.OnPhotoCaptured(uri, invId))
                },
                isPremium = sharedViewModel.businessSettings.collectAsStateWithLifecycle(initialValue = BusinessSettings()).value.isPremium,
                platformActions = platformActions
            )
        }
        composable<Screen.Suppliers> {
            val uiState by suppliersViewModel.uiState.collectAsStateWithLifecycle()
            val isAddSheetVisible by suppliersViewModel.isAddSheetVisible.collectAsStateWithLifecycle()
            val placeSuggestions by suppliersViewModel.placeSuggestions.collectAsStateWithLifecycle()
            val isReorderMode by suppliersViewModel.isReorderMode.collectAsStateWithLifecycle()
            val reorderList by suppliersViewModel.reorderList.collectAsStateWithLifecycle()
            val hiddenSuppliers by suppliersViewModel.hiddenSuppliers.collectAsStateWithLifecycle()
            
            SuppliersTab(
                uiState = uiState,
                isAddSheetVisible = isAddSheetVisible,
                placeSuggestions = placeSuggestions,
                isReorderMode = isReorderMode,
                reorderList = reorderList,
                onTogglePin = { suppliersViewModel.togglePin(it) },
                onHideSupplier = { suppliersViewModel.hideSupplier(it) },
                onAddClick = { suppliersViewModel.setAddSheetVisible(true) },
                onDismissAdd = { suppliersViewModel.setAddSheetVisible(false) },
                onAddSupplier = { name, cat, addr, phone, logo -> 
                    suppliersViewModel.addSupplier(name, cat, addr, phone, logo)
                },
                onSearchQueryChange = { suppliersViewModel.onSearchQueryChange(it) },
                onClearSuggestions = { suppliersViewModel.clearSuggestions() },
                onToggleReorder = { active, list -> suppliersViewModel.setReorderMode(active, list) },
                onMoveItem = { from, to -> suppliersViewModel.swapItems(from, to) },
                onSaveOrder = { suppliersViewModel.saveOrder() },
                hiddenSuppliers = hiddenSuppliers,
                onRestoreSupplier = { suppliersViewModel.restoreSupplier(it) },
                onLogPurchase = { id, name, amt, desc -> 
                    suppliersViewModel.logPurchase(id, amt)
                },
                onOpenStore = { url -> platformActions.openUrl(url) },
                onSnapPhoto = { suppliersViewModel.onPhotoCaptured(it) },
                photoUri = suppliersViewModel.capturedPhotoUri.collectAsStateWithLifecycle().value,
                platformActions = platformActions
            )
        }
        composable<Screen.Settings> {
            val settings by sharedViewModel.businessSettings.collectAsStateWithLifecycle(initialValue = BusinessSettings())
            val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
            val syncState by authViewModel.syncState.collectAsStateWithLifecycle()
            
            SettingsTab(
                settings = settings,
                currentUser = currentUser,
                onSaveSettings = { sharedViewModel.saveBusinessSettings(it) },
                onSignIn = { 
                    // This is a fallback if someone navigates directly to Settings route
                    platformActions.showToast("Sign in via the Main Tab bar.")
                },
                onSignOut = { authViewModel.signOut() },
                onSync = { authViewModel.triggerBackup() },
                syncState = syncState,
                platformActions = platformActions
            )
        }
    }
}
