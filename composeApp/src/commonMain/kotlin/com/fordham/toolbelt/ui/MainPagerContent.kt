package com.fordham.toolbelt.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fordham.toolbelt.domain.model.BusinessSettings
import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.PaymentRequestType
import com.fordham.toolbelt.ui.tabs.ClientsTab
import com.fordham.toolbelt.ui.tabs.HistoryTab
import com.fordham.toolbelt.ui.tabs.NewInvoiceTab
import com.fordham.toolbelt.ui.tabs.ReceiptsTab
import com.fordham.toolbelt.ui.tabs.SettingsTab
import com.fordham.toolbelt.ui.tabs.StatsTab
import com.fordham.toolbelt.ui.tabs.SuppliersTab
import com.fordham.toolbelt.ui.viewmodel.AuthViewModel
import com.fordham.toolbelt.ui.viewmodel.ClientsIntent
import com.fordham.toolbelt.ui.viewmodel.ClientsViewModel
import com.fordham.toolbelt.ui.viewmodel.HistoryViewModel
import com.fordham.toolbelt.ui.viewmodel.NewInvoiceIntent
import com.fordham.toolbelt.ui.viewmodel.NewInvoiceUiState
import com.fordham.toolbelt.ui.viewmodel.NewInvoiceViewModel
import com.fordham.toolbelt.ui.viewmodel.PaymentViewModel
import com.fordham.toolbelt.ui.viewmodel.ReceiptsViewModel
import com.fordham.toolbelt.ui.viewmodel.SettingsViewModel
import com.fordham.toolbelt.ui.viewmodel.SharedViewModel
import com.fordham.toolbelt.ui.viewmodel.StatsViewModel
import com.fordham.toolbelt.ui.viewmodel.SuppliersViewModel
import com.fordham.toolbelt.util.UiMessageKeys
import com.fordham.toolbelt.ui.localizeUiMessage
import com.fordham.toolbelt.util.PlatformActions
import org.jetbrains.compose.resources.stringResource
import invoicehammer.composeapp.generated.resources.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainPagerContent(
    pagerState: PagerState,
    newInvoiceUiState: NewInvoiceUiState,
    businessSettings: BusinessSettings,
    newInvoiceViewModel: NewInvoiceViewModel,
    historyViewModel: HistoryViewModel,
    receiptsViewModel: ReceiptsViewModel,
    statsViewModel: StatsViewModel,
    clientsViewModel: ClientsViewModel,
    suppliersViewModel: SuppliersViewModel,
    authViewModel: AuthViewModel,
    settingsViewModel: SettingsViewModel,
    onOpenPaywall: () -> Unit,
    sharedViewModel: SharedViewModel,
    paymentViewModel: PaymentViewModel,
    platformActions: PlatformActions,
    onNavigateToSettings: () -> Unit,
    onChoosePaymentMethod: (Invoice, PaymentRequestType) -> Unit,
    paymentRequests: List<com.fordham.toolbelt.domain.model.InvoicePaymentRequest> = emptyList(),
    blockPagerScroll: Boolean = false
) {
    val bentoShareTitle = stringResource(Res.string.bento_report)
    val taxShareTitle = stringResource(Res.string.tax_bundle)
    var pendingToastKey by remember { mutableStateOf<String?>(null) }
    val localizedToast = pendingToastKey?.let { localizeUiMessage(it) }

    LaunchedEffect(localizedToast) {
        localizedToast?.let {
            platformActions.showToast(it)
            pendingToastKey = null
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = !blockPagerScroll
    ) { page ->
        when (page) {
            0 -> NewInvoiceTab(
                uiState = newInvoiceUiState,
                businessSettings = businessSettings,
                allClients = clientsViewModel.allClients.collectAsStateWithLifecycle(initialValue = emptyList()).value,
                categories = newInvoiceViewModel.categories,
                onSaveBusinessSettings = { sharedViewModel.saveBusinessSettings(it) },
                onTimerToggle = { newInvoiceViewModel.onIntent(NewInvoiceIntent.ToggleTimer) },
                onHourlyRateChange = { newInvoiceViewModel.onIntent(NewInvoiceIntent.OnHourlyRateChange(it)) },
                onBillLabor = { newInvoiceViewModel.onIntent(NewInvoiceIntent.BillLabor) },
                onLogoUriChange = { newInvoiceViewModel.onIntent(NewInvoiceIntent.OnLogoUriChange(it)) },
                onPhotoCaptured = { uri, phase ->
                    newInvoiceViewModel.onIntent(NewInvoiceIntent.OnPhotoCaptured(uri, phase))
                },
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
                onSaveInvoice = { isEst, set, comp -> newInvoiceViewModel.onIntent(NewInvoiceIntent.SaveInvoice(isEst, set, comp)) },
                onLinkReceipt = { r, m -> newInvoiceViewModel.onIntent(NewInvoiceIntent.LinkReceipt(r, m)) },
                onShareFile = { f, t -> platformActions.shareFile(f, t) },
                isPremium = businessSettings.isPremium,
                platformActions = platformActions
            )
            1 -> HistoryTab(
                uiState = historyViewModel.uiState.collectAsStateWithLifecycle().value,
                filteredHistory = historyViewModel.filteredInvoices.collectAsStateWithLifecycle(initialValue = emptyList()).value,
                paymentRequests = paymentRequests,
                onViewPdf = { platformActions.openPdf(it) },
                onSharePdf = { f, t -> platformActions.shareFile(f, t) },
                onRequestDeposit = { onChoosePaymentMethod(it, PaymentRequestType.Deposit) },
                onRequestFullPayment = { onChoosePaymentMethod(it, PaymentRequestType.FullBalance) },
                onSetInvoiceToDelete = { historyViewModel.setInvoiceToDelete(it) },
                onDeleteInvoice = { historyViewModel.deleteInvoice(it) },
                onSearchQueryChange = { historyViewModel.onSearchQueryChange(it) },
                onShowPaidOnlyChange = { historyViewModel.onShowPaidOnlyChange(it) },
                onUpdateInvoice = { historyViewModel.updateInvoice(it) },
                onConvertEstimateToInvoice = { historyViewModel.convertEstimateToInvoice(it) },
                onSendAiReminder = { historyViewModel.selectInvoiceForReminder(it) },
                platformActions = platformActions,
                listScrollEnabled = !blockPagerScroll
            )
            2 -> {
                val selectedClient by sharedViewModel.selectedClient.collectAsStateWithLifecycle()
                ReceiptsTab(
                    uiState = receiptsViewModel.uiState.collectAsStateWithLifecycle().value,
                    selectedClient = selectedClient,
                    allClients = sharedViewModel.allClients.collectAsStateWithLifecycle(initialValue = emptyList()).value,
                    filteredReceipts = receiptsViewModel.filteredReceipts.collectAsStateWithLifecycle(initialValue = emptyList()).value,
                    receiptsTotal = receiptsViewModel.receiptsTotal.collectAsStateWithLifecycle(initialValue = 0.0).value,
                    totalWithMarkup = receiptsViewModel.totalWithMarkup.collectAsStateWithLifecycle(initialValue = 0.0).value,
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
                    onAcceptExpenseMatch = { receiptsViewModel.acceptExpenseMatch() },
                    onAcceptSingleExpenseMatch = { receiptsViewModel.acceptSingleExpenseMatch(it) },
                    onDeclineExpenseMatch = { receiptsViewModel.declineExpenseMatch() },
                    platformActions = platformActions
                )
            }
            3 -> StatsTab(
                stats = statsViewModel.businessStats.collectAsStateWithLifecycle().value,
                settings = businessSettings,
                onExportCsv = {
                    statsViewModel.exportBentoReport { path, savedTo ->
                        savedTo?.let { pendingToastKey = UiMessageKeys.savedTo(it) }
                        platformActions.shareFile(path, bentoShareTitle)
                    }
                },
                onExportZip = {
                    statsViewModel.exportTaxBundle { path, savedTo ->
                        savedTo?.let { pendingToastKey = UiMessageKeys.savedTo(it) }
                        platformActions.shareFile(path, taxShareTitle)
                    }
                },
                onNavigateToSettings = onNavigateToSettings,
                onInsertStressInvoices = { statsViewModel.createStressTestInvoices() },
                onEraseAllInvoices = { statsViewModel.eraseAllInvoices() }
            )
            4 -> SuppliersTab(
                uiState = suppliersViewModel.uiState.collectAsState().value,
                isAddSheetVisible = suppliersViewModel.isAddSheetVisible.collectAsState().value,
                placeSuggestions = suppliersViewModel.placeSuggestions.collectAsState().value,
                isReorderMode = suppliersViewModel.isReorderMode.collectAsState().value,
                reorderList = suppliersViewModel.reorderList.collectAsState().value,
                onTogglePin = { suppliersViewModel.togglePin(it) },
                onHideSupplier = { suppliersViewModel.hideSupplier(it) },
                onAddClick = { suppliersViewModel.setAddSheetVisible(true) },
                onDismissAdd = { suppliersViewModel.setAddSheetVisible(false) },
                onAddSupplier = { n, c, a, p, w, l -> suppliersViewModel.addSupplier(n, c, a, p, w, l) },
                onSearchQueryChange = { suppliersViewModel.onSearchQueryChange(it) },
                onClearSuggestions = { suppliersViewModel.clearSuggestions() },
                onToggleReorder = { a, l -> suppliersViewModel.setReorderMode(a, l) },
                onMoveItem = { f, t -> suppliersViewModel.swapItems(f, t) },
                onSaveOrder = { suppliersViewModel.saveOrder() },
                hiddenSuppliers = suppliersViewModel.hiddenSuppliers.collectAsState().value,
                onRestoreSupplier = { suppliersViewModel.restoreSupplier(it) },
                onLogPurchase = { id, _, amt, _ -> suppliersViewModel.logPurchase(id, amt) },
                onOpenStore = { platformActions.openUrl(it) },
                onSnapPhoto = { suppliersViewModel.onPhotoCaptured(it) },
                photoUri = suppliersViewModel.capturedPhotoUri.collectAsState().value,
                platformActions = platformActions
            )
            5 -> {
                val notes by sharedViewModel.selectedClientNotes.collectAsStateWithLifecycle(initialValue = emptyList())
                ClientsTab(
                    clients = clientsViewModel.allClients.collectAsStateWithLifecycle(initialValue = emptyList()).value,
                    selectedClient = sharedViewModel.selectedClient.collectAsStateWithLifecycle().value,
                    clientInvoices = sharedViewModel.selectedClientInvoices.collectAsStateWithLifecycle().value,
                    summary = sharedViewModel.selectedClientSummary.collectAsStateWithLifecycle().value,
                    jobNotes = notes,
                    jobPhotos = sharedViewModel.selectedClientPhotos.collectAsStateWithLifecycle().value,
                    uiState = clientsViewModel.uiState.collectAsStateWithLifecycle().value,
                    onClientClick = { sharedViewModel.selectClient(it) },
                    onDeleteClient = { clientsViewModel.onIntent(ClientsIntent.DeleteClient(it)) },
                    onSetClientToDelete = { clientsViewModel.onIntent(ClientsIntent.SetClientToDelete(it)) },
                    onBackClick = { sharedViewModel.selectClient(null) },
                    onAddNote = { clientsViewModel.onIntent(ClientsIntent.AddNote(it)) },
                    onDeleteNote = { clientsViewModel.onIntent(ClientsIntent.DeleteNote(it)) },
                    onSummarizeNotes = { clientsViewModel.onIntent(ClientsIntent.SummarizeNotes(notes)) },
                    onLinkReceipt = { r -> sharedViewModel.selectedClient.value?.let { clientsViewModel.onIntent(ClientsIntent.LinkReceipt(r, it.name.value)) } },
                    onViewPdf = { platformActions.openPdf(it) },
                    onSetNoteText = { clientsViewModel.onIntent(ClientsIntent.OnNoteTextChange(it)) },
                    onSetAddNoteVisible = { clientsViewModel.onIntent(ClientsIntent.SetAddNoteVisible(it)) },
                    onSetReceiptPickerVisible = { clientsViewModel.onIntent(ClientsIntent.SetReceiptPickerVisible(it)) },
                    onClearAiSummary = { clientsViewModel.onIntent(ClientsIntent.ClearAiSummary) },
                    onCallClient = { platformActions.callPhone(it) },
                    onEmailClient = { platformActions.sendEmail(it) },
                    onPhotoCaptured = { uri, invId, phase ->
                        clientsViewModel.onIntent(ClientsIntent.OnPhotoCaptured(uri, invId, phase))
                    },
                    isPremium = businessSettings.isPremium,
                    platformActions = platformActions
                )
            }
            6 -> {
                val currentUser = authViewModel.currentUser.collectAsStateWithLifecycle().value
                LaunchedEffect(Unit) { settingsViewModel.onSettingsTabVisible() }
                LaunchedEffect(currentUser?.id?.value) {
                    if (currentUser != null) settingsViewModel.refreshConnectStatus()
                }
                val stripeConnectState by settingsViewModel.connectState.collectAsStateWithLifecycle()
                val stripeConnectBusy by settingsViewModel.connectBusy.collectAsStateWithLifecycle()
                val isLlamaDownloaded by settingsViewModel.isLlamaDownloaded.collectAsStateWithLifecycle()
                val isLlamaDownloading by settingsViewModel.isLlamaDownloading.collectAsStateWithLifecycle()
                val llamaDownloadProgress by settingsViewModel.llamaDownloadProgress.collectAsStateWithLifecycle()
                SettingsTab(
                settings = businessSettings,
                currentUser = currentUser,
                isLlamaDownloaded = isLlamaDownloaded,
                isLlamaDownloading = isLlamaDownloading,
                llamaDownloadProgress = llamaDownloadProgress,
                onDownloadLlama = { settingsViewModel.downloadLlama() },
                onDeleteLlama = { settingsViewModel.deleteLlama() },
                stripePaymentMode = settingsViewModel.stripePaymentMode,
                stripeConnectState = stripeConnectState,
                stripeConnectBusy = stripeConnectBusy,
                onRefreshStripeConnect = { settingsViewModel.refreshConnectStatus() },
                onStartStripeConnectOnboarding = {
                    settingsViewModel.startConnectOnboarding(
                        onOpenUrl = { platformActions.openUrl(it) },
                        onMessage = { pendingToastKey = it }
                    )
                },
                onSaveSettings = { sharedViewModel.saveBusinessSettings(it) },
                onSignIn = {
                    authViewModel.clearAuthMessage()
                    platformActions.signInWithGoogle(
                        onSuccess = { authViewModel.signIn(it) },
                        onError = { pendingToastKey = it }
                    )
                },
                onSignOut = { authViewModel.signOut() },
                onSync = { authViewModel.triggerBackup() },
                onRestore = { authViewModel.triggerRestore() },
                onOpenPaywall = onOpenPaywall,
                isPro = businessSettings.isPremium,
                syncState = authViewModel.syncState.collectAsStateWithLifecycle().value,
                supabaseConnectionMode = authViewModel.supabaseConnectionMode,
                platformActions = platformActions,
                onPickBusinessLogo = {
                    platformActions.pickImage { uri ->
                        uri?.let { sharedViewModel.saveBusinessLogo(it) }
                    }
                },
                onRemoveBusinessLogo = { sharedViewModel.saveBusinessLogo(null) }
            )
            }
        }
    }
}
