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
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
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
    val historyUiState by historyViewModel.uiState.collectAsStateWithLifecycle()
    val paymentRequests = remember(paymentState.requests) { paymentState.requests }
    val currentBusinessSettings by sharedViewModel.businessSettings.collectAsStateWithLifecycle(initialValue = BusinessSettings())

    var pendingLogoToast by remember { mutableStateOf<String?>(null) }
    var pendingAuthToast by remember { mutableStateOf<String?>(null) }
    var pendingPaymentToast by remember { mutableStateOf<String?>(null) }
    var showPaywall by remember { mutableStateOf(false) }

    val localizedLogoToast = pendingLogoToast?.let { localizeUiMessage(it) }
    val localizedAuthToast = pendingAuthToast?.let { localizeUiMessage(it) }
    val localizedPaymentToast = pendingPaymentToast?.let { localizeUiMessage(it) }
    val spokenAgentResponse = agentState.lastResponse?.let { localizeUiMessage(it) }

    LaunchedEffect(spokenAgentResponse) {
        spokenAgentResponse?.let { voiceAssistant.speak(it) }
    }

    LaunchedEffect(newInvoiceUiState.userSummary) {
        val summary = newInvoiceUiState.userSummary
        if (summary.isNotBlank()) {
            voiceAssistant.speak(summary)
        }
    }

    LaunchedEffect(Unit) {
        sharedViewModel.logoMessage.collect { pendingLogoToast = it }
    }

    LaunchedEffect(localizedLogoToast) {
        localizedLogoToast?.let {
            platformActions.showToast(it)
            pendingLogoToast = null
        }
    }

    val authMessage by authViewModel.authMessage.collectAsStateWithLifecycle()
    LaunchedEffect(authMessage) {
        authMessage?.let { pendingAuthToast = it }
        authViewModel.clearAuthMessage()
    }
 
    LaunchedEffect(currentBusinessSettings) {
        if (!currentBusinessSettings.isPremium && !currentBusinessSettings.hasSeenPreLaunchPaywall) {
            delay(1500)
            showPaywall = true
            sharedViewModel.saveBusinessSettings(
                currentBusinessSettings.copy(hasSeenPreLaunchPaywall = true)
            )
        }
    }
 
    LaunchedEffect(localizedAuthToast) {
        localizedAuthToast?.let {
            platformActions.showToast(it)
            pendingAuthToast = null
        }
    }

    val paymentError = paymentState.errorMessage
    LaunchedEffect(paymentError) {
        paymentError?.let { pendingPaymentToast = it }
        paymentViewModel.clearError()
    }

    val paymentSuccess = paymentState.paymentSuccessMessage
    LaunchedEffect(paymentSuccess) {
        paymentSuccess?.let { pendingPaymentToast = it }
        paymentViewModel.clearPaymentSuccessMessage()
    }

    LaunchedEffect(localizedPaymentToast) {
        localizedPaymentToast?.let {
            platformActions.showToast(it)
            pendingPaymentToast = null
        }
    }

    LifecycleResumeEffect(Unit) {
        paymentViewModel.refreshPendingStripeCheckout()
        onPauseOrDispose { }
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
    val subscriptionUiState by subscriptionViewModel.uiState.collectAsStateWithLifecycle()
    val receiptsUiState by receiptsViewModel.uiState.collectAsStateWithLifecycle()
    val canUseForeman = subscriptionUiState.canUseForemanAgent
    val isLocalLlmReady by settingsViewModel.isLlamaDownloaded.collectAsStateWithLifecycle()
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
        val activeTab = com.fordham.toolbelt.domain.model.agent.AppTab.entries
            .firstOrNull { it.pageIndex == pagerState.currentPage }
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
            selectedClientName = client?.name?.value,
            knownClientsCatalog = knownClientsCatalog,
            knownSuppliersCatalog = knownSuppliersCatalog,
            pendingReceiptImageBytes = pendingReceiptBytes,
            lastSavedInvoiceId = runtimeBinding.lastSavedInvoiceId,
            lastSavedInvoiceClientName = runtimeBinding.lastSavedInvoiceClientName,
            voiceTranscriptMeta = voiceTranscriptMeta,
            activeTab = activeTab
        )
    }

    val startAgentListening = {
        voiceAssistant.stopSpeaking()
        if (platformActions.isPermissionGranted(Permission.RECORD_AUDIO)) {
            agentViewModel.clearAgentResponse()
            agentViewModel.setAgentActive(true)
            agentViewModel.setListening(true)
            voiceAssistant.startListeningWithMeta(
                onResult = { meta ->
                    agentViewModel.setListening(false)
                    AppLogger.e(
                        "VoiceInvoiceHandoff",
                        "FINAL_TO_FOREMAN text=${VoiceInvoiceLogRedactor.transcriptMeta(meta.text)} confidence=${meta.confidence} " +
                            "activeTab=${ForemanRuntimeBinding.current().activeTab} " +
                            "canUseForeman=$canUseForeman localLlmReady=$isLocalLlmReady"
                    )
                    scope.launch {
                        agentViewModel.executeAgentCommand(
                            meta.text,
                            buildForemanAppContext(voiceTranscriptMeta = meta),
                            handleAgentIntent,
                            handleAgentEffect
                        )
                    }
                },
                onEnd = { agentViewModel.setListening(false) },
                onPartialResult = { partial ->
                    AppLogger.e("VoiceInvoiceHandoff", "PARTIAL text=${VoiceInvoiceLogRedactor.transcriptMeta(partial)}")
                    agentViewModel.updateTranscript(partial)
                }
            )
        } else {
            platformActions.requestPermission(Permission.RECORD_AUDIO) {}
        }
    }

    val stopAgentListening: (Boolean) -> Unit = { discard ->
        agentViewModel.setListening(false)
        voiceAssistant.stopListening(discard = discard)
        voiceAssistant.stopSpeaking()
    }

    val toggleAgentListening = {
        if (agentState.isListening) {
            stopAgentListening(false)
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
                    showLedgerIcon = pagerState.currentPage != 0,
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
                    isPremium = canUseForeman || isLocalLlmReady,
                    onStartListening = {
                        startAgentListening()
                    },
                    onStopListening = {
                        stopAgentListening(false)
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
                        stopAgentListening(true)
                        agentViewModel.clearAgentResponse()
                    },
                    onStartAgentListening = toggleAgentListening,
                    scope = scope,
                    buildForemanAppContext = { buildForemanAppContext() },
                    handleAgentIntent = handleAgentIntent,
                    handleAgentEffect = handleAgentEffect,
                    platformActions = platformActions,
                    historyUiState = historyUiState,
                    onDismissReminder = { historyViewModel.selectInvoiceForReminder(null) },
                    onToneChange = { historyViewModel.updateReminderTone(it) },
                    onChannelChange = { historyViewModel.updateReminderChannel(it) },
                    onGenerateReminder = {
                        val activeRequest = paymentRequests.firstOrNull { it.invoiceId == historyUiState.reminderInvoice?.id }
                        historyViewModel.generateReminder(
                            contractorName = currentBusinessSettings.businessName,
                            paymentLink = activeRequest?.paymentLink?.value
                        )
                    },
                    onUpdateGeneratedText = { subject, body ->
                        historyViewModel.updateGeneratedText(subject, body)
                    },
                    onSendShareReminder = {
                        platformActions.shareText(
                            text = historyUiState.generatedBody,
                            subject = historyUiState.generatedSubject,
                            recipientEmail = historyUiState.reminderInvoice?.clientEmail?.value.orEmpty(),
                            recipientPhone = historyUiState.reminderInvoice?.clientPhone?.value.orEmpty()
                        )
                    },
                    onBetaUnlock = {
                        val newSettings = currentBusinessSettings.copy(isPremium = true)
                        sharedViewModel.saveBusinessSettings(newSettings)
                        showPaywall = false
                    }
                )
            }
        }
    }
}
