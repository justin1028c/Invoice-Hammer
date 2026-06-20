package com.fordham.toolbelt.ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.PaymentProviderType
import com.fordham.toolbelt.domain.model.PaymentRequestType
import com.fordham.toolbelt.domain.model.ClientId
import com.fordham.toolbelt.domain.model.agent.AgentUiEffect
import com.fordham.toolbelt.domain.model.agent.ForemanAppContextBundle
import com.fordham.toolbelt.domain.model.AiAgentIntent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.fordham.toolbelt.domain.model.stripe.StripePaymentMode
import com.fordham.toolbelt.ui.components.*
import com.fordham.toolbelt.ui.viewmodel.*
import com.fordham.toolbelt.ui.localizeUiMessage
import com.fordham.toolbelt.util.PlatformActions

@Composable
fun BoxScope.MainScreenOverlays(
    showPaymentLedger: Boolean,
    onDismissPaymentLedger: () -> Unit,
    paymentState: PaymentUiState,
    pendingPaymentInvoice: Invoice?,
    pendingPaymentType: PaymentRequestType?,
    onClearPendingPayment: () -> Unit,
    onStartCardTerminal: (Invoice, PaymentRequestType) -> Unit,
    onShowPremiumLock: () -> Unit,
    cardTerminalInvoice: Invoice?,
    cardTerminalType: PaymentRequestType?,
    onClearCardTerminal: () -> Unit,
    cardTerminalState: CardTerminalUiState,
    stripePaymentMode: StripePaymentMode,
    paymentViewModel: PaymentViewModel,
    showPaywall: Boolean,
    onDismissPaywall: () -> Unit,
    subscriptionUiState: SubscriptionUiState,
    subscriptionViewModel: SubscriptionViewModel,
    agentViewModel: AgentViewModel,
    agentState: AgentUiState,
    onDismissAgent: () -> Unit,
    onStartAgentListening: () -> Unit,
    scope: CoroutineScope,
    buildForemanAppContext: suspend () -> ForemanAppContextBundle,
    handleAgentIntent: (AiAgentIntent) -> Unit,
    handleAgentEffect: (AgentUiEffect) -> Unit,
    platformActions: PlatformActions,
    historyUiState: HistoryUiState,
    onDismissReminder: () -> Unit,
    onToneChange: (com.fordham.toolbelt.domain.usecase.ReminderTone) -> Unit,
    onChannelChange: (com.fordham.toolbelt.domain.usecase.ReminderChannel) -> Unit,
    onGenerateReminder: () -> Unit,
    onUpdateGeneratedText: (String, String) -> Unit,
    onSendShareReminder: () -> Unit
) {
    var pendingToastKey by remember { mutableStateOf<String?>(null) }
    val localizedToast = pendingToastKey?.let { localizeUiMessage(it) }

    LaunchedEffect(localizedToast) {
        localizedToast?.let {
            platformActions.showToast(it)
            pendingToastKey = null
        }
    }

    if (showPaymentLedger) {
        PaymentLedgerSheet(
            uiState = paymentState,
            onDismiss = onDismissPaymentLedger,
            onOpenPaymentLink = { platformActions.openUrl(it) }
        )
    }

    val invoiceForPayment = pendingPaymentInvoice
    val paymentType = pendingPaymentType
    if (invoiceForPayment != null && paymentType != null) {
        PaymentMethodPickerSheet(
            requestType = paymentType,
            onDismiss = onClearPendingPayment,
            onProviderSelected = { provider ->
                when (provider) {
                    PaymentProviderType.CardTerminal -> {
                        paymentViewModel.resetCardTerminal()
                        onStartCardTerminal(invoiceForPayment, paymentType)
                    }
                    PaymentProviderType.TapToPay -> {
                        paymentViewModel.collectTapToPay(
                            invoice = invoiceForPayment,
                            type = paymentType,
                            onSuccess = onClearPendingPayment,
                            onError = { msg ->
                                pendingToastKey = msg
                                onClearPendingPayment()
                            }
                        )
                    }
                    PaymentProviderType.BluetoothReader -> {
                        paymentViewModel.collectBluetoothReader(
                            invoice = invoiceForPayment,
                            type = paymentType,
                            onSuccess = onClearPendingPayment,
                            onPremiumRequired = onShowPremiumLock,
                            onError = { msg ->
                                pendingToastKey = msg
                                onClearPendingPayment()
                            }
                        )
                    }
                    else -> {
                        paymentViewModel.createRequest(
                            invoice = invoiceForPayment,
                            type = paymentType,
                            provider = provider,
                            onFinished = onClearPendingPayment
                        )
                        return@PaymentMethodPickerSheet
                    }
                }
                if (provider != PaymentProviderType.TapToPay &&
                    provider != PaymentProviderType.BluetoothReader &&
                    provider != PaymentProviderType.CardTerminal
                ) {
                    onClearPendingPayment()
                }
            }
        )
    }

    val terminalInvoice = cardTerminalInvoice
    val terminalType = cardTerminalType
    if (terminalInvoice != null && terminalType != null) {
        CardTerminalSheet(
            invoice = terminalInvoice,
            requestType = terminalType,
            uiState = cardTerminalState,
            stripePaymentMode = stripePaymentMode,
            onDismiss = {
                paymentViewModel.resetCardTerminal()
                onClearCardTerminal()
            },
            onDraftChange = paymentViewModel::updateCardTerminalDraft,
            onSecureCheckout = {
                paymentViewModel.chargeStripePaymentSheet(terminalInvoice, terminalType) {
                    onClearCardTerminal()
                    paymentViewModel.resetCardTerminal()
                }
            },
            onManualCharge = {
                paymentViewModel.chargeCardTerminal(terminalInvoice, terminalType) {
                    onClearCardTerminal()
                    paymentViewModel.resetCardTerminal()
                }
            }
        )
    }

    if (showPaywall) {
        PaywallSheet(
            uiState = subscriptionUiState,
            onDismiss = onDismissPaywall,
            onPurchase = { subscriptionViewModel.purchase(it) },
            onPurchaseCreditPack = { subscriptionViewModel.purchaseProduct(it) },
            onRestore = { subscriptionViewModel.restore() }
        )
    }

    paymentState.latestRequest?.let { request ->
        PaymentRequestCreatedDialog(
            request = request,
            isLivePowerPay = paymentState.isLivePowerPay,
            isLiveStripe = stripePaymentMode == StripePaymentMode.PaymentSheet,
            checkoutUrl = paymentState.activeCheckoutUrl ?: request.paymentLink.value,
            checkoutLinkCanPay = paymentState.checkoutLinkCanPay,
            checkoutLinkMessage = paymentState.checkoutLinkMessage,
            isResolvingCheckoutLink = paymentState.isResolvingCheckoutLink,
            onDismiss = { paymentViewModel.clearLatestRequest() },
            onOpenPaymentLink = { platformActions.openUrl(it) },
            onRegenerateLink = { paymentViewModel.regenerateCheckoutLink() }
        )
    }

    AgentOverlay(
        isActive = agentState.isActive,
        isProcessing = agentState.isProcessing,
        currentMode = agentState.currentMode,
        transcript = agentState.transcript,
        lastResponse = agentState.lastResponse,
        isListening = agentState.isListening,
        typedCommand = agentState.typedCommand,
        onTypedCommandChange = agentViewModel::updateTypedCommand,
        onSendTypedCommand = {
            scope.launch {
                agentViewModel.executeTypedCommand(
                    buildForemanAppContext(),
                    handleAgentIntent,
                    handleAgentEffect
                )
            }
        },
        onDismiss = onDismissAgent,
        onMicClick = onStartAgentListening,
        onApprove = {
            scope.launch {
                agentViewModel.approveToolCall(
                    buildForemanAppContext(),
                    handleAgentIntent,
                    handleAgentEffect
                )
            }
        },
        pendingApproval = agentState.pendingApproval,
        clientChoices = agentState.clientChoices,
        onSelectClient = { clientId: ClientId ->
            scope.launch {
                agentViewModel.selectClientAndContinue(
                    clientId,
                    buildForemanAppContext(),
                    handleAgentIntent,
                    handleAgentEffect
                )
            }
        },
        onDismissClientChoices = agentViewModel::dismissClientChoices,
        savePreview = agentState.savePreview,
        onConfirmSave = {
            scope.launch {
                agentViewModel.confirmSaveAndContinue(
                    buildForemanAppContext(),
                    handleAgentIntent,
                    handleAgentEffect
                )
            }
        },
        onDismissSavePreview = agentViewModel::dismissSavePreview,
        stepSummaries = agentState.stepSummaries,
        modifier = Modifier.align(Alignment.BottomStart).imePadding()
    )

    if (historyUiState.showReminderSheet) {
        historyUiState.reminderInvoice?.let { invoice ->
            AiReminderSheet(
                invoice = invoice,
                tone = historyUiState.reminderTone,
                channel = historyUiState.reminderChannel,
                generatedSubject = historyUiState.generatedSubject,
                generatedBody = historyUiState.generatedBody,
                isGenerating = historyUiState.isGeneratingReminder,
                error = historyUiState.reminderError,
                onToneChange = onToneChange,
                onChannelChange = onChannelChange,
                onGenerate = onGenerateReminder,
                onUpdateGeneratedText = onUpdateGeneratedText,
                onSendShare = onSendShareReminder,
                onDismiss = onDismissReminder
            )
        }
    }
}
