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
import com.fordham.toolbelt.ui.components.*
import com.fordham.toolbelt.ui.theme.ToolbeltTheme
import com.fordham.toolbelt.ui.viewmodel.*
import com.fordham.toolbelt.util.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

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
    voiceAssistant: VoiceAssistant,
    platformActions: PlatformActions,
    initialPage: Int = 0
) {
    val scope = rememberCoroutineScope()
    val newInvoiceUiState by newInvoiceViewModel.uiState.collectAsStateWithLifecycle()
    val agentState by agentViewModel.uiState.collectAsStateWithLifecycle()
    val paymentState by paymentViewModel.uiState.collectAsStateWithLifecycle()
    val currentBusinessSettings by sharedViewModel.businessSettings.collectAsStateWithLifecycle(initialValue = BusinessSettings())

    LaunchedEffect(agentState.lastResponse) {
        agentState.lastResponse?.let { voiceAssistant.speak(it) }
    }

    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 7 })
    var showPremiumLockDialog by remember { mutableStateOf(false) }
    var showPaymentLedger by remember { mutableStateOf(false) }
    var pendingPaymentInvoice by remember { mutableStateOf<Invoice?>(null) }
    var pendingPaymentType by remember { mutableStateOf<PaymentRequestType?>(null) }

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
            else -> {}
        }
    }

    val startAgentListening = {
        if (platformActions.isPermissionGranted(Permission.RECORD_AUDIO)) {
            agentViewModel.setListening(true)
            voiceAssistant.startListening(
                onResult = { command ->
                    agentViewModel.setListening(false)
                    val appContext = "Current Tab: ${pagerState.currentPage}, Client Selected: ${sharedViewModel.selectedClient.value?.name ?: "None"}"
                    agentViewModel.executeAgentCommand(command, appContext, handleAgentIntent)
                },
                onEnd = { agentViewModel.setListening(false) }
            )
        } else {
            platformActions.requestPermission(Permission.RECORD_AUDIO) {}
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
                    isPremium = currentBusinessSettings.isPremium,
                    onStartListening = {
                        agentViewModel.setAgentActive(true)
                        startAgentListening()
                    },
                    onStopListening = { agentViewModel.setAgentActive(false) },
                    onPremiumRequired = { showPremiumLockDialog = true }
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
                    sharedViewModel = sharedViewModel,
                    paymentViewModel = paymentViewModel,
                    platformActions = platformActions,
                    onNavigateToSettings = { scope.launch { pagerState.animateScrollToPage(6) } },
                    onChoosePaymentMethod = { invoice, type ->
                        pendingPaymentInvoice = invoice
                        pendingPaymentType = type
                    }
                )

                MainScreenDialogs(
                    newInvoiceUiState = newInvoiceUiState,
                    showPremiumLock = showPremiumLockDialog,
                    statsError = statsViewModel.errorMessage.collectAsStateWithLifecycle().value,
                    agentError = agentState.errorMessage,
                    onDismissPremium = { showPremiumLockDialog = false },
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

                if (showPaymentLedger) {
                    PaymentLedgerSheet(
                        uiState = paymentState,
                        onDismiss = { showPaymentLedger = false },
                        onOpenPaymentLink = { platformActions.openUrl(it) }
                    )
                }

                val invoiceForPayment = pendingPaymentInvoice
                val paymentType = pendingPaymentType
                if (invoiceForPayment != null && paymentType != null) {
                    PaymentMethodPickerSheet(
                        requestType = paymentType,
                        onDismiss = {
                            pendingPaymentInvoice = null
                            pendingPaymentType = null
                        },
                        onProviderSelected = { provider ->
                            paymentViewModel.createRequest(invoiceForPayment, paymentType, provider)
                            pendingPaymentInvoice = null
                            pendingPaymentType = null
                        }
                    )
                }

                paymentState.latestRequest?.let { request ->
                    PaymentRequestCreatedDialog(
                        request = request,
                        onDismiss = { paymentViewModel.clearLatestRequest() },
                        onOpenPaymentLink = { platformActions.openUrl(it) }
                    )
                }

                AgentOverlay(
                    isActive = agentState.isActive,
                    isProcessing = agentState.isProcessing,
                    currentMode = agentState.currentMode,
                    transcript = agentState.transcript,
                    lastResponse = agentState.lastResponse,
                    isListening = agentState.isListening,
                    onDismiss = { agentViewModel.setAgentActive(false) },
                    onMicClick = { startAgentListening() },
                    onApprove = { agentViewModel.approveToolCall() },
                    pendingApproval = agentState.pendingApproval,
                    modifier = Modifier.align(Alignment.BottomStart)
                )
            }
        }
    }
}
