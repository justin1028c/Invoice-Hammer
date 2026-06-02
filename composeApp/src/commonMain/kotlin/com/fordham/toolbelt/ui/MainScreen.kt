package com.fordham.toolbelt.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.model.agent.AgentUiEffect
import com.fordham.toolbelt.domain.model.agent.AppTab
import com.fordham.toolbelt.domain.model.agent.ForemanAppContextBundle
import com.fordham.toolbelt.domain.model.agent.ForemanRuntimeBinding
import com.fordham.toolbelt.navigation.MainTabNavigation
import com.fordham.toolbelt.ui.components.*
import com.fordham.toolbelt.ui.theme.ToolbeltTheme
import com.fordham.toolbelt.ui.viewmodel.*
import com.fordham.toolbelt.util.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

/**
 * Responsibility: Main application shell, orchestrating navigation and global state.
 * ADHERENCE: Below 300 line limit.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    newInvoiceViewModel: NewInvoiceViewModel,
    historyViewModel: HistoryViewModel,
    receiptsViewModel: ReceiptsViewModel,
    statsViewModel: StatsViewModel,
    clientsViewModel: ClientsViewModel,
    suppliersViewModel: SuppliersViewModel,
    agentViewModel: AgentViewModel,
    authViewModel: AuthViewModel,
    paymentViewModel: PaymentViewModel,
    sharedViewModel: SharedViewModel,
    subscriptionViewModel: SubscriptionViewModel,
    settingsViewModel: SettingsViewModel,
    voiceAssistant: VoiceAssistant,
    platformActions: PlatformActions,
    initialPage: Int = 0
) {
    val scope = rememberCoroutineScope()
    val newInvoiceUiState by newInvoiceViewModel.uiState.collectAsStateWithLifecycle()
    val agentState by agentViewModel.uiState.collectAsStateWithLifecycle()
    val paymentState by paymentViewModel.uiState.collectAsStateWithLifecycle()
    val paymentRequests = remember(paymentState.requests) { paymentState.requests }
    val currentBusinessSettings by sharedViewModel.businessSettings.collectAsStateWithLifecycle(initialValue = BusinessSettings())

    LaunchedEffect(agentState.lastResponse) {
        agentState.lastResponse?.let { voiceAssistant.speak(it) }
    }

    LaunchedEffect(Unit) {
        sharedViewModel.logoMessage.collect { message ->
            platformActions.showToast(message)
        }
    }

    val authMessage by authViewModel.authMessage.collectAsStateWithLifecycle()
    LaunchedEffect(authMessage) {
        authMessage?.let { message ->
            platformActions.showToast(message)
            authViewModel.clearAuthMessage()
        }
    }

    val paymentError = paymentState.errorMessage
    LaunchedEffect(paymentError) {
        paymentError?.let { message ->
            platformActions.showToast(message)
            paymentViewModel.clearError()
        }
    }

    val paymentOpenUrl = paymentState.openUrlOnce
    LaunchedEffect(paymentOpenUrl) {
        paymentOpenUrl?.let { url ->
            platformActions.openUrl(url)
            paymentViewModel.consumeOpenUrl()
        }
    }

    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 7 })

    val pendingTab by MainTabNavigation.pendingTab.collectAsStateWithLifecycle()

    LaunchedEffect(pendingTab) {
        pendingTab?.let { tab ->
            pagerState.animateScrollToPage(tab.pageIndex)
            MainTabNavigation.clear()
        }
    }

    var showPremiumLockDialog by remember { mutableStateOf(false) }
    var showPaywall by remember { mutableStateOf(false) }
    val subscriptionUiState by subscriptionViewModel.uiState.collectAsStateWithLifecycle()
    val receiptsUiState by receiptsViewModel.uiState.collectAsStateWithLifecycle()
    val canUseForeman = subscriptionUiState.canUseForemanAgent
    var showPaymentLedger by remember { mutableStateOf(false) }
    var pendingPaymentInvoice by remember { mutableStateOf<Invoice?>(null) }
    var pendingPaymentType by remember { mutableStateOf<PaymentRequestType?>(null) }
    var cardTerminalInvoice by remember { mutableStateOf<Invoice?>(null) }
    var cardTerminalType by remember { mutableStateOf<PaymentRequestType?>(null) }
    val cardTerminalState by paymentViewModel.cardTerminal.collectAsStateWithLifecycle()
    val paymentOverlayOpen = pendingPaymentInvoice != null || cardTerminalInvoice != null

    val handleAgentEffect: (AgentUiEffect) -> Unit = { effect ->
        when (effect) {
            is AgentUiEffect.NavigateToTab -> scope.launch { pagerState.animateScrollToPage(effect.tab.pageIndex) }
            is AgentUiEffect.SelectClient -> sharedViewModel.selectClientById(effect.clientId)
            is AgentUiEffect.SearchHistory -> {
                scope.launch { pagerState.animateScrollToPage(AppTab.History.pageIndex) }
                historyViewModel.onSearchQueryChange(effect.query)
            }
            is AgentUiEffect.ShareInvoiceDocument -> platformActions.shareDocument(
                path = effect.pdfPath,
                title = effect.title,
                recipientEmail = effect.recipientEmail,
                recipientPhone = effect.recipientPhone,
                subject = effect.subject,
                body = effect.body
            )
            is AgentUiEffect.ViewPdf -> {
                platformActions.openPdf(effect.pdfPath)
            }
            is AgentUiEffect.OpenSupplierStore -> {
                scope.launch { pagerState.animateScrollToPage(AppTab.Suppliers.pageIndex) }
                platformActions.launchApp(
                    packageName = effect.packageName,
                    fallbackUrl = effect.webUrl
                )
            }
        }
    }

    val handleAgentIntent: (AiAgentIntent) -> Unit = { intent ->
        when (intent) {
            is AiAgentIntent.DraftInvoice -> {
                scope.launch { pagerState.animateScrollToPage(0) }
                intent.data?.let { newInvoiceViewModel.onIntent(NewInvoiceIntent.OnItemDescChange(it)) }
            }
            is AiAgentIntent.SummarizeClient -> {
                scope.launch { pagerState.animateScrollToPage(5) }
                sharedViewModel.selectClientByName(intent.clientName)
            }
            is AiAgentIntent.AnalyzeFinances -> scope.launch { pagerState.animateScrollToPage(3) }
            is AiAgentIntent.FindJob -> {
                scope.launch { pagerState.animateScrollToPage(1) }
                historyViewModel.onSearchQueryChange(intent.query)
            }
            is AiAgentIntent.ScanReceipt -> scope.launch { pagerState.animateScrollToPage(2) }
            is AiAgentIntent.OpenStores -> scope.launch { pagerState.animateScrollToPage(4) }
            is AiAgentIntent.PremiumRequired -> showPremiumLockDialog = true
            is AiAgentIntent.StepByStepInvoiceCommit -> {
                scope.launch { pagerState.animateScrollToPage(0) }
                newInvoiceViewModel.onIntent(NewInvoiceIntent.OnClientNameChange(intent.clientName))
                newInvoiceViewModel.onIntent(NewInvoiceIntent.OnClientAddressChange(intent.clientAddress))
                newInvoiceViewModel.onIntent(NewInvoiceIntent.OnCategoryChange(intent.category))
                newInvoiceViewModel.onIntent(NewInvoiceIntent.OnItemDescChange(intent.description))
                newInvoiceViewModel.onIntent(NewInvoiceIntent.OnItemAmtChange(intent.amount.toString()))
                newInvoiceViewModel.onIntent(NewInvoiceIntent.AddManualLineItem)
            }
            else -> {}
        }
    }

    suspend fun buildForemanAppContext(
        voiceTranscriptMeta: com.fordham.toolbelt.util.VoiceTranscriptMeta? = null
    ): ForemanAppContextBundle {
        val draft = newInvoiceUiState
        val client = sharedViewModel.selectedClient.value
        val clients = runCatching { sharedViewModel.allClients.first().take(20) }.getOrDefault(emptyList())
        val knownClientsCatalog = if (clients.isNotEmpty()) {
            buildString {
                append("KNOWN_CLIENT_CATALOG:\n")
                clients.forEach { c ->
                    append("- ${c.name} (id=${c.id.value})\n")
                }
            }
        } else {
            ""
        }

        val suppliersState = suppliersViewModel.uiState.value
        val suppliers = if (suppliersState is com.fordham.toolbelt.ui.viewmodel.SuppliersOutcome.Success) {
            suppliersState.data.pinnedSuppliers + suppliersState.data.activeSuppliers
        } else {
            emptyList()
        }
        val knownSuppliersCatalog = if (suppliers.isNotEmpty()) {
            buildString {
                append("KNOWN_SUPPLIER_CATALOG:\n")
                suppliers.forEach { s ->
                    append("- ${s.domain.name} (id=${s.domain.id.value}, category=${s.domain.category.name})\n")
                }
            }
        } else {
            ""
        }

        val runtimeBinding = ForemanRuntimeBinding.current()
        val pendingReceiptBytes = receiptsUiState.capturedImageBytes
        return buildForemanAppContextBundle(
            buildSystemPrompt = {
                buildForemanSystemPrompt(
                    tabIndex = pagerState.currentPage,
                    selectedClient = client,
                    draft = draft,
                    lastSavedInvoiceId = runtimeBinding.lastSavedInvoiceId?.value,
                    lastSavedInvoiceClient = runtimeBinding.lastSavedInvoiceClientName,
                    pendingReceiptPhoto = pendingReceiptBytes != null,
                    catalogClients = clients,
                    session = agentViewModel.session,
                    voiceTranscriptMeta = voiceTranscriptMeta
                )
            },
            selectedClientId = client?.id,
            selectedClientName = client?.name,
            knownClientsCatalog = knownClientsCatalog,
            knownSuppliersCatalog = knownSuppliersCatalog,
            pendingReceiptImageBytes = pendingReceiptBytes,
            lastSavedInvoiceId = runtimeBinding.lastSavedInvoiceId,
            lastSavedInvoiceClientName = runtimeBinding.lastSavedInvoiceClientName,
            voiceTranscriptMeta = voiceTranscriptMeta
        )
    }

    val startAgentListening = {
        if (platformActions.isPermissionGranted(Permission.RECORD_AUDIO)) {
            agentViewModel.setListening(true)
            voiceAssistant.startListeningWithMeta(
                onResult = { meta ->
                    agentViewModel.setListening(false)
                    scope.launch {
                        agentViewModel.executeAgentCommand(
                            meta.text,
                            buildForemanAppContext(voiceTranscriptMeta = meta),
                            handleAgentIntent,
                            handleAgentEffect
                        )
                    }
                },
                onEnd = { agentViewModel.setListening(false) }
            )
        } else {
            platformActions.requestPermission(Permission.RECORD_AUDIO) {}
        }
    }

    val stopAgentListening = {
        agentViewModel.setListening(false)
        voiceAssistant.stopListening()
    }

    val toggleAgentListening = {
        if (agentState.isListening) {
            stopAgentListening()
        } else {
            startAgentListening()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceAssistant.destroy()
        }
    }


    ToolbeltTheme(darkTheme = currentBusinessSettings.isDarkMode) {
        Scaffold(
            topBar = { 
                MainTopBar(
                    onLedgerClick = { showPaymentLedger = true },
                    onSettingsClick = { scope.launch { pagerState.animateScrollToPage(6) } }
                )
            },
            bottomBar = {
                MainBottomBar(
                    currentPage = pagerState.currentPage,
                    onTabSelected = { scope.launch { pagerState.animateScrollToPage(it) } }
                )
            },
            floatingActionButton = {
                MainAgentFab(
                    isListening = agentState.isListening,
                    isPremium = canUseForeman,
                    onStartListening = {
                        agentViewModel.setAgentActive(true)
                        startAgentListening()
                    },
                    onStopListening = {
                        agentViewModel.setAgentActive(false)
                        stopAgentListening()
                    },
                    onPremiumRequired = {
                        if (subscriptionUiState.entitlement?.hasFeature(
                                com.fordham.toolbelt.domain.model.subscription.SubscriptionFeature.ForemanAgent
                            ) != true
                        ) {
                            showPaywall = true
                        } else {
                            showPremiumLockDialog = true
                        }
                    }
                )
            }
        ) { inner ->
            Box(modifier = Modifier.padding(inner).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                MainPagerContent(
                    pagerState = pagerState,
                    newInvoiceUiState = newInvoiceUiState,
                    businessSettings = currentBusinessSettings,
                    newInvoiceViewModel = newInvoiceViewModel,
                    historyViewModel = historyViewModel,
                    receiptsViewModel = receiptsViewModel,
                    statsViewModel = statsViewModel,
                    clientsViewModel = clientsViewModel,
                    suppliersViewModel = suppliersViewModel,
                    authViewModel = authViewModel,
                    settingsViewModel = settingsViewModel,
                    onOpenPaywall = { showPaywall = true },
                    sharedViewModel = sharedViewModel,
                    paymentViewModel = paymentViewModel,
                    platformActions = platformActions,
                    onNavigateToSettings = { scope.launch { pagerState.animateScrollToPage(6) } },
                    onChoosePaymentMethod = { invoice, type ->
                        pendingPaymentInvoice = invoice
                        pendingPaymentType = type
                    },
                    paymentRequests = paymentRequests,
                    blockPagerScroll = paymentOverlayOpen
                )

                MainScreenDialogs(
                    newInvoiceUiState = newInvoiceUiState,
                    showPremiumLock = showPremiumLockDialog,
                    statsError = statsViewModel.errorMessage.collectAsStateWithLifecycle().value,
                    agentError = agentState.errorMessage,
                    onDismissPremium = { showPremiumLockDialog = false },
                    onOpenPaywall = {
                        showPremiumLockDialog = false
                        showPaywall = true
                    },
                    onGoToSettings = {
                        showPremiumLockDialog = false
                        scope.launch { pagerState.animateScrollToPage(6) }
                    },
                    onDismissAiConf = { newInvoiceViewModel.onIntent(NewInvoiceIntent.OnShowAiConfChange(false)) },
                    onAcceptAi = { newInvoiceViewModel.onIntent(NewInvoiceIntent.AcceptAiItems) },
                    onDismissNewInvoiceError = { newInvoiceViewModel.onIntent(NewInvoiceIntent.ClearError) },
                    onDismissStatsError = { statsViewModel.clearError() },
                    onDismissAgentError = { agentViewModel.clearAgentResponse() }
                )

                MainScreenOverlays(
                    showPaymentLedger = showPaymentLedger,
                    onDismissPaymentLedger = { showPaymentLedger = false },
                    paymentState = paymentState,
                    pendingPaymentInvoice = pendingPaymentInvoice,
                    pendingPaymentType = pendingPaymentType,
                    onClearPendingPayment = {
                        pendingPaymentInvoice = null
                        pendingPaymentType = null
                    },
                    onStartCardTerminal = { invoice, type ->
                        cardTerminalInvoice = invoice
                        cardTerminalType = type
                    },
                    onShowPremiumLock = { showPremiumLockDialog = true },
                    cardTerminalInvoice = cardTerminalInvoice,
                    cardTerminalType = cardTerminalType,
                    onClearCardTerminal = {
                        cardTerminalInvoice = null
                        cardTerminalType = null
                    },
                    cardTerminalState = cardTerminalState,
                    stripePaymentMode = paymentViewModel.stripePaymentMode,
                    paymentViewModel = paymentViewModel,
                    showPaywall = showPaywall,
                    onDismissPaywall = {
                        showPaywall = false
                        subscriptionViewModel.clearMessage()
                    },
                    subscriptionUiState = subscriptionUiState,
                    subscriptionViewModel = subscriptionViewModel,
                    agentViewModel = agentViewModel,
                    agentState = agentState,
                    onDismissAgent = {
                        stopAgentListening()
                        agentViewModel.clearAgentResponse()
                    },
                    onStartAgentListening = toggleAgentListening,
                    scope = scope,
                    buildForemanAppContext = { buildForemanAppContext() },
                    handleAgentIntent = handleAgentIntent,
                    handleAgentEffect = handleAgentEffect,
                    platformActions = platformActions
                )
            }
        }
    }
}
