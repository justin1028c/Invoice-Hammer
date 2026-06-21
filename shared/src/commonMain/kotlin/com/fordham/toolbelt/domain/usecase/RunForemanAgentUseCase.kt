package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.ClientId
import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.model.agent.AgentOutcome
import com.fordham.toolbelt.domain.model.agent.AgentRole
import com.fordham.toolbelt.domain.model.agent.ChainedToolStep
import com.fordham.toolbelt.domain.model.agent.ForemanAgentRun
import com.fordham.toolbelt.domain.model.agent.ForemanChainLimits
import com.fordham.toolbelt.domain.model.agent.ForemanSession
import com.fordham.toolbelt.domain.model.agent.ForemanSessionCompressor
import com.fordham.toolbelt.domain.model.agent.ForemanSessionReducer
import com.fordham.toolbelt.domain.model.agent.ForemanCommandRouter
import com.fordham.toolbelt.domain.model.agent.ForemanRoute
import com.fordham.toolbelt.domain.model.agent.ForemanTabNavigation
import com.fordham.toolbelt.domain.model.agent.ForemanToolPolicy
import com.fordham.toolbelt.domain.model.agent.ForemanToolResultSummarizer
import com.fordham.toolbelt.domain.model.agent.OpenTabArgs
import com.fordham.toolbelt.domain.model.agent.ForemanTurn
import com.fordham.toolbelt.domain.model.agent.InvoiceSavePreview
import com.fordham.toolbelt.domain.model.agent.NaturalLanguage
import com.fordham.toolbelt.domain.model.agent.SaveInvoiceFromDraftArgs
import com.fordham.toolbelt.domain.model.agent.SelectClientArgs
import com.fordham.toolbelt.domain.model.agent.QuickInvoiceArgs
import com.fordham.toolbelt.domain.model.agent.QuickClientAndInvoiceArgs
import com.fordham.toolbelt.domain.model.agent.TimestampMillis
import com.fordham.toolbelt.domain.model.agent.ToolArgumentValidator
import com.fordham.toolbelt.domain.model.agent.ToolArguments
import com.fordham.toolbelt.domain.model.agent.ToolExecutionResult
import com.fordham.toolbelt.domain.model.agent.ToolCallId
import com.fordham.toolbelt.domain.model.agent.ToolName
import com.fordham.toolbelt.domain.model.agent.ToolSafety
import com.fordham.toolbelt.util.randomUUID
import com.fordham.toolbelt.domain.model.subscription.PremiumFeature
import com.fordham.toolbelt.domain.model.subscription.SubscriptionFeature
import com.fordham.toolbelt.domain.model.subscription.TokenConsumptionOutcome
import com.fordham.toolbelt.domain.repository.AgentLlmGateway
import com.fordham.toolbelt.domain.repository.DraftRepository
import com.fordham.toolbelt.domain.repository.ForemanAgentDispatchers
import com.fordham.toolbelt.domain.repository.ToolRegistry
import com.fordham.toolbelt.domain.repository.SettingsRepository
import com.fordham.toolbelt.domain.usecase.subscription.ConsumeTokenUseCase
import com.fordham.toolbelt.domain.usecase.subscription.HasSubscriptionFeatureUseCase
import com.fordham.toolbelt.util.PlatformActions
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class RunForemanAgentUseCase(
    private val llmGateway: AgentLlmGateway,
    private val toolRegistry: ToolRegistry,
    private val draftRepository: DraftRepository,
    private val dispatchers: ForemanAgentDispatchers,
    private val hasSubscriptionFeature: HasSubscriptionFeatureUseCase,
    private val consumeToken: ConsumeTokenUseCase,
    private val platformActions: PlatformActions,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(
        command: NaturalLanguage,
        session: ForemanSession,
        systemPrompt: NaturalLanguage,
        timestamp: TimestampMillis = TimestampMillis(Clock.System.now().toEpochMilliseconds())
    ): ForemanAgentRun = withContext(dispatchers.background) {
        if (command.value.isBlank()) {
            return@withContext ForemanAgentRun(
                outcome = AgentOutcome.Failure(FailureMessage("Agent command cannot be blank.")),
                session = session
            )
        }
        foremanAccessFailure()?.let { denial ->
            return@withContext ForemanAgentRun(outcome = denial, session = session)
        }
        val workingSession = session.append(
            ForemanTurn(role = AgentRole.User, content = command, timestamp = timestamp)
        )
        val autoSaveEnabled = settingsRepository.getBusinessSettings().autoSaveVoiceInvoices
        when (val route = ForemanCommandRouter.route(command.value, autoSaveEnabled)) {
            is ForemanRoute.LocalTab -> return@withContext runNavigationFastPath(
                workingSession,
                route.tab,
                timestamp
            )
            is ForemanRoute.LocalMacro -> return@withContext runMacroFastPath(
                workingSession,
                route.toolName,
                route.arguments,
                timestamp
            )
            is ForemanRoute.LlmChain -> Unit
        }
        runLoop(workingSession, systemPrompt, timestamp, mutableListOf())
    }

    private suspend fun runNavigationFastPath(
        session: ForemanSession,
        tab: com.fordham.toolbelt.domain.model.agent.AppTab,
        timestamp: TimestampMillis
    ): ForemanAgentRun {
        val toolCallId = ToolCallId(randomUUID())
        val request = AgentOutcome.ToolExecutionRequested(
            toolCallId = toolCallId,
            toolName = ToolName.OpenTab,
            arguments = OpenTabArgs(tab)
        )
        val result = executeTool(ToolName.OpenTab, OpenTabArgs(tab))
        val sessionAfter = recordToolStep(session, request, result, timestamp)
        return ForemanAgentRun(
            outcome = AgentOutcome.ToolChainExecuted(
                steps = listOf(ChainedToolStep(toolCallId, ToolName.OpenTab, result)),
                finalMessage = ForemanToolResultSummarizer.tabOpenedUserMessage(tab)
            ),
            session = sessionAfter
        )
    }

    private suspend fun runMacroFastPath(
        session: ForemanSession,
        toolName: ToolName,
        arguments: ToolArguments,
        timestamp: TimestampMillis
    ): ForemanAgentRun {
        val validationFailure = validateToolRequest(toolName, arguments)
        if (validationFailure != null) {
            return ForemanAgentRun(validationFailure, session)
        }

        if (toolName == ToolName.SaveInvoiceFromDraft && arguments is SaveInvoiceFromDraftArgs) {
            val preview = buildSavePreview(arguments)
            if (preview != null) {
                return ForemanAgentRun(
                    AgentOutcome.SaveConfirmationRequired(
                        preview = preview,
                        pendingToolCallId = ToolCallId(randomUUID()),
                        pendingArguments = arguments,
                        completedSteps = emptyList()
                    ),
                    session
                )
            }
        }
        if (toolName == ToolName.QuickInvoice && arguments is QuickInvoiceArgs) {
            val total = arguments.lineItems.sumOf { it.amount }
            if (total >= 500.0) {
                return ForemanAgentRun(
                    AgentOutcome.RequiresApproval(
                        toolCallId = ToolCallId(randomUUID()),
                        toolName = toolName,
                        arguments = arguments,
                        completedSteps = emptyList()
                    ),
                    session
                )
            }
        }
        if (toolName == ToolName.QuickClientAndInvoice && arguments is QuickClientAndInvoiceArgs) {
            val total = arguments.lineItems.sumOf { it.amount }
            if (total >= 500.0) {
                return ForemanAgentRun(
                    AgentOutcome.RequiresApproval(
                        toolCallId = ToolCallId(randomUUID()),
                        toolName = toolName,
                        arguments = arguments,
                        completedSteps = emptyList()
                    ),
                    session
                )
            }
        }

        val toolCallId = ToolCallId(randomUUID())
        val request = AgentOutcome.ToolExecutionRequested(
            toolCallId = toolCallId,
            toolName = toolName,
            arguments = arguments
        )
        val result = executeTool(toolName, arguments)
        if (result is ToolExecutionResult.Failure) {
            val sessionAfter = recordToolStep(session, request, result, timestamp)
            return ForemanAgentRun(
                outcome = AgentOutcome.ToolChainExecuted(
                    steps = listOf(ChainedToolStep(toolCallId, toolName, result)),
                    finalMessage = ForemanToolResultSummarizer.retryExhaustedMessage(result.error)
                ),
                session = sessionAfter
            )
        }

        if (result is ToolExecutionResult.ClientSearchCompleted && result.clients.size in 2..6) {
            val sessionAfter = recordToolStep(session, request, result, timestamp)
            return ForemanAgentRun(
                AgentOutcome.ClientChoiceRequired(
                    candidates = result.clients,
                    completedSteps = listOf(ChainedToolStep(toolCallId, toolName, result))
                ),
                sessionAfter
            )
        }

        val sessionAfter = recordToolStep(session, request, result, timestamp)
        return ForemanAgentRun(
            outcome = AgentOutcome.ToolChainExecuted(
                steps = listOf(ChainedToolStep(toolCallId, toolName, result)),
                finalMessage = ForemanToolResultSummarizer.toUserSummary(
                    listOf(ChainedToolStep(toolCallId, toolName, result))
                )
            ),
            session = sessionAfter
        )
    }

    /** Resume after approval, client pick, or save confirm — no new user utterance. */
    suspend fun continueChain(
        session: ForemanSession,
        systemPrompt: NaturalLanguage,
        completedSteps: List<ChainedToolStep>,
        timestamp: TimestampMillis = TimestampMillis(Clock.System.now().toEpochMilliseconds())
    ): ForemanAgentRun = withContext(dispatchers.background) {
        foremanAccessFailure()?.let { denial ->
            return@withContext ForemanAgentRun(outcome = denial, session = session)
        }
        runLoop(session, systemPrompt, timestamp, completedSteps.toMutableList())
    }

    suspend fun continueAfterApproval(
        pending: AgentOutcome.RequiresApproval,
        session: ForemanSession,
        systemPrompt: NaturalLanguage,
        completedSteps: List<ChainedToolStep>,
        timestamp: TimestampMillis = TimestampMillis(Clock.System.now().toEpochMilliseconds())
    ): ForemanAgentRun = withContext(dispatchers.background) {
        foremanAccessFailure()?.let { denial ->
            return@withContext ForemanAgentRun(outcome = denial, session = session)
        }
        val steps = completedSteps.toMutableList()
        executeStepAndContinue(
            session = session,
            systemPrompt = systemPrompt,
            completedSteps = steps,
            toolCallId = pending.toolCallId,
            toolName = pending.toolName,
            arguments = pending.arguments,
            timestamp = timestamp
        )
    }

    suspend fun continueAfterClientPick(
        clientId: ClientId,
        session: ForemanSession,
        systemPrompt: NaturalLanguage,
        completedSteps: List<ChainedToolStep>,
        timestamp: TimestampMillis = TimestampMillis(Clock.System.now().toEpochMilliseconds())
    ): ForemanAgentRun = withContext(dispatchers.background) {
        foremanAccessFailure()?.let { denial ->
            return@withContext ForemanAgentRun(outcome = denial, session = session)
        }
        val steps = completedSteps.toMutableList()
        executeStepAndContinue(
            session = session,
            systemPrompt = systemPrompt,
            completedSteps = steps,
            toolCallId = ToolCallId(randomUUID()),
            toolName = ToolName.SelectClient,
            arguments = SelectClientArgs(clientId = clientId),
            timestamp = timestamp
        )
    }

    suspend fun continueAfterSaveConfirm(
        pending: AgentOutcome.SaveConfirmationRequired,
        session: ForemanSession,
        systemPrompt: NaturalLanguage,
        completedSteps: List<ChainedToolStep>,
        timestamp: TimestampMillis = TimestampMillis(Clock.System.now().toEpochMilliseconds())
    ): ForemanAgentRun = withContext(dispatchers.background) {
        foremanAccessFailure()?.let { denial ->
            return@withContext ForemanAgentRun(outcome = denial, session = session)
        }
        val steps = completedSteps.toMutableList()
        executeStepAndContinue(
            session = session,
            systemPrompt = systemPrompt,
            completedSteps = steps,
            toolCallId = pending.pendingToolCallId,
            toolName = ToolName.SaveInvoiceFromDraft,
            arguments = pending.pendingArguments,
            timestamp = timestamp
        )
    }

    private suspend fun executeStepAndContinue(
        session: ForemanSession,
        systemPrompt: NaturalLanguage,
        completedSteps: MutableList<ChainedToolStep>,
        toolCallId: ToolCallId,
        toolName: ToolName,
        arguments: ToolArguments,
        timestamp: TimestampMillis
    ): ForemanAgentRun {
        val validationFailure = validateToolRequest(toolName, arguments)
        if (validationFailure != null) {
            return ForemanAgentRun(validationFailure, session)
        }

        val request = AgentOutcome.ToolExecutionRequested(
            toolCallId = toolCallId,
            toolName = toolName,
            arguments = arguments
        )
        val result = executeTool(toolName, arguments)
        completedSteps.add(ChainedToolStep(toolCallId, toolName, result))
        val sessionAfter = recordToolStep(session, request, result, timestamp)
        if (result is ToolExecutionResult.Failure) {
            return ForemanAgentRun(
                outcome = AgentOutcome.ToolChainExecuted(
                    steps = completedSteps.toList(),
                    finalMessage = ForemanToolResultSummarizer.retryExhaustedMessage(result.error)
                ),
                session = sessionAfter
            )
        }
        val autoSaveEnabled = settingsRepository.getBusinessSettings().autoSaveVoiceInvoices
        if (toolName == ToolName.SaveInvoiceFromDraft ||
            !autoSaveEnabled && (
                toolName == ToolName.QuickInvoice ||
                toolName == ToolName.QuickClientAndInvoice ||
                toolName == ToolName.QuickInvoiceFromUnbilledReceipts ||
                toolName == ToolName.DuplicateLastInvoice ||
                toolName == ToolName.UpdateDraftInvoice ||
                toolName == ToolName.AppendDraftLines
            )
        ) {
            return ForemanAgentRun(
                outcome = AgentOutcome.ToolChainExecuted(
                    steps = completedSteps.toList(),
                    finalMessage = ForemanToolResultSummarizer.toUserSummary(completedSteps)
                ),
                session = sessionAfter
            )
        }
        return runLoop(sessionAfter, systemPrompt, timestamp, completedSteps)
    }

    private suspend fun runLoop(
        initialSession: ForemanSession,
        systemPrompt: NaturalLanguage,
        timestamp: TimestampMillis,
        completedSteps: MutableList<ChainedToolStep>
    ): ForemanAgentRun {
        var workingSession = initialSession
        var iterations = 0
        var pendingRetryTool: ToolName? = null
        val executedCalls = mutableListOf<Pair<ToolName, ToolArguments>>()

        while (iterations < ForemanChainLimits.MAX_SAFE_STEPS_PER_COMMAND) {
            iterations++
            workingSession = ForemanSessionCompressor.compressForPrompt(workingSession)
            val latestCommand = latestUserCommand(workingSession)
            val skill = com.fordham.toolbelt.domain.model.agent.AgentSkillClassifier.classify(latestCommand)
            val filteredFunctions = toolRegistry.availableFunctions().filter { it.toolName in skill.allowedTools }

            when (val outcome = llmGateway.prompt(
                systemPrompt = systemPrompt,
                session = workingSession,
                functions = filteredFunctions
            )) {
                is AgentOutcome.TextResponse -> {
                    val finalOutcome = if (completedSteps.isEmpty()) {
                        outcome
                    } else {
                        AgentOutcome.ToolChainExecuted(
                            steps = completedSteps.toList(),
                            finalMessage = ForemanToolResultSummarizer.toUserSummary(
                                completedSteps,
                                outcome.response
                            )
                        )
                    }
                    return ForemanAgentRun(finalOutcome, workingSession)
                }
                is AgentOutcome.Failure -> {
                    val finalOutcome = if (completedSteps.isEmpty()) {
                        outcome
                    } else {
                        AgentOutcome.ToolChainExecuted(
                            steps = completedSteps.toList(),
                            finalMessage = ForemanToolResultSummarizer.toUserSummary(
                                completedSteps,
                                NaturalLanguage(outcome.error.value)
                            )
                        )
                    }
                    return ForemanAgentRun(finalOutcome, workingSession)
                }
                is AgentOutcome.ToolExecuted,
                is AgentOutcome.ToolChainExecuted,
                is AgentOutcome.ClientChoiceRequired,
                is AgentOutcome.SaveConfirmationRequired,
                is AgentOutcome.TabNavigationCompleted -> {
                    return ForemanAgentRun(
                        AgentOutcome.Failure(FailureMessage("Unexpected outcome from LLM gateway.")),
                        workingSession
                    )
                }
                is AgentOutcome.RequiresApproval -> {
                    val validationFailure = validateToolRequest(outcome.toolName, outcome.arguments)
                    if (validationFailure != null) {
                        return ForemanAgentRun(validationFailure, workingSession)
                    }
                    return ForemanAgentRun(
                        outcome.copy(completedSteps = completedSteps.toList()),
                        workingSession
                    )
                }
                is AgentOutcome.ToolExecutionRequested -> {
                    val validationFailure = validateToolRequest(outcome.toolName, outcome.arguments)
                    if (validationFailure != null) {
                        return ForemanAgentRun(validationFailure, workingSession)
                    }

                    val callKey = Pair(outcome.toolName, outcome.arguments)
                    if (executedCalls.count { it == callKey } >= 2) {
                        return ForemanAgentRun(
                            outcome = AgentOutcome.ToolChainExecuted(
                                steps = completedSteps.toList(),
                                finalMessage = NaturalLanguage(
                                    "Detected a tool loop. Stopping execution to prevent runaway costs. " +
                                    ForemanToolResultSummarizer.toUserSummary(completedSteps).value
                                )
                            ),
                            session = workingSession
                        )
                    }
                    executedCalls.add(callKey)

                    when (ForemanToolPolicy.safetyFor(outcome.toolName)) {
                        ToolSafety.RequiresApproval -> {
                            return ForemanAgentRun(
                                AgentOutcome.RequiresApproval(
                                    toolCallId = outcome.toolCallId,
                                    toolName = outcome.toolName,
                                    arguments = outcome.arguments,
                                    completedSteps = completedSteps.toList()
                                ),
                                workingSession
                            )
                        }
                        ToolSafety.Safe -> {
                            if (outcome.toolName == ToolName.SaveInvoiceFromDraft &&
                                outcome.arguments is SaveInvoiceFromDraftArgs
                            ) {
                                val preview = buildSavePreview(outcome.arguments)
                                if (preview != null) {
                                    return ForemanAgentRun(
                                        AgentOutcome.SaveConfirmationRequired(
                                            preview = preview,
                                            pendingToolCallId = outcome.toolCallId,
                                            pendingArguments = outcome.arguments,
                                            completedSteps = completedSteps.toList()
                                        ),
                                        workingSession
                                    )
                                }
                            }
                            if (outcome.toolName == ToolName.QuickInvoice && outcome.arguments is QuickInvoiceArgs) {
                                val total = outcome.arguments.lineItems.sumOf { it.amount }
                                if (total >= 500.0) {
                                    return ForemanAgentRun(
                                        AgentOutcome.RequiresApproval(
                                            toolCallId = outcome.toolCallId,
                                            toolName = outcome.toolName,
                                            arguments = outcome.arguments,
                                            completedSteps = completedSteps.toList()
                                        ),
                                        workingSession
                                    )
                                }
                            }
                            if (outcome.toolName == ToolName.QuickClientAndInvoice && outcome.arguments is QuickClientAndInvoiceArgs) {
                                val total = outcome.arguments.lineItems.sumOf { it.amount }
                                if (total >= 500.0) {
                                    return ForemanAgentRun(
                                        AgentOutcome.RequiresApproval(
                                            toolCallId = outcome.toolCallId,
                                            toolName = outcome.toolName,
                                            arguments = outcome.arguments,
                                            completedSteps = completedSteps.toList()
                                        ),
                                        workingSession
                                    )
                                }
                            }

                            val result = executeTool(outcome.toolName, outcome.arguments)
                            if (result is ToolExecutionResult.Failure) {
                                val step = ChainedToolStep(outcome.toolCallId, outcome.toolName, result)
                                completedSteps.add(step)
                                workingSession = recordToolStep(
                                    workingSession, outcome, result, timestamp
                                )
                                if (pendingRetryTool == outcome.toolName) {
                                    return ForemanAgentRun(
                                        outcome = AgentOutcome.ToolChainExecuted(
                                            steps = completedSteps.toList(),
                                            finalMessage = ForemanToolResultSummarizer.retryExhaustedMessage(
                                                result.error
                                            )
                                        ),
                                        session = workingSession
                                    )
                                }
                                pendingRetryTool = outcome.toolName
                                continue
                            }

                            pendingRetryTool = null

                            val isListOrSummarize = latestUserCommand(workingSession).lowercase().let { cmd ->
                                cmd.contains("summarize") || 
                                cmd.contains("summarise") || 
                                cmd.contains("list") || 
                                cmd.contains("all") || 
                                cmd.contains("recent") || 
                                cmd.contains("last") ||
                                cmd.contains("show") ||
                                cmd.contains("find") ||
                                cmd.contains("search") ||
                                cmd.contains("get") ||
                                cmd.contains("view") ||
                                cmd.contains("who")
                            }

                            if (result is ToolExecutionResult.ClientSearchCompleted &&
                                result.clients.size in 2..6 &&
                                !isListOrSummarize
                            ) {
                                val step = ChainedToolStep(outcome.toolCallId, outcome.toolName, result)
                                completedSteps.add(step)
                                workingSession = recordToolStep(
                                    workingSession, outcome, result, timestamp
                                )
                                return ForemanAgentRun(
                                    AgentOutcome.ClientChoiceRequired(
                                        candidates = result.clients,
                                        completedSteps = completedSteps.toList()
                                    ),
                                    workingSession
                                )
                            }

                            val step = ChainedToolStep(outcome.toolCallId, outcome.toolName, result)
                            completedSteps.add(step)
                            workingSession = recordToolStep(
                                workingSession, outcome, result, timestamp
                            )
                            val autoSaveEnabled = settingsRepository.getBusinessSettings().autoSaveVoiceInvoices
                            if (outcome.toolName == ToolName.SaveInvoiceFromDraft ||
                                !autoSaveEnabled && (
                                    outcome.toolName == ToolName.QuickInvoice ||
                                    outcome.toolName == ToolName.QuickClientAndInvoice ||
                                    outcome.toolName == ToolName.QuickInvoiceFromUnbilledReceipts ||
                                    outcome.toolName == ToolName.DuplicateLastInvoice ||
                                    outcome.toolName == ToolName.UpdateDraftInvoice ||
                                    outcome.toolName == ToolName.AppendDraftLines
                                )
                            ) {
                                return ForemanAgentRun(
                                    outcome = AgentOutcome.ToolChainExecuted(
                                        steps = completedSteps.toList(),
                                        finalMessage = ForemanToolResultSummarizer.toUserSummary(completedSteps)
                                    ),
                                    session = workingSession
                                )
                            }
                            if (outcome.toolName == ToolName.OpenTab &&
                                result is ToolExecutionResult.TabOpened &&
                                ForemanTabNavigation.isNavigationOnly(latestUserCommand(workingSession))
                            ) {
                                return ForemanAgentRun(
                                    outcome = AgentOutcome.ToolChainExecuted(
                                        steps = completedSteps.toList(),
                                        finalMessage = ForemanToolResultSummarizer.tabOpenedUserMessage(result.tab)
                                    ),
                                    session = workingSession
                                )
                            }
                        }
                    }
                }
            }
        }

        return ForemanAgentRun(
            outcome = if (completedSteps.isEmpty()) {
                AgentOutcome.Failure(
                    FailureMessage(
                        "Agent stopped after ${ForemanChainLimits.MAX_SAFE_STEPS_PER_COMMAND} steps without finishing."
                    )
                )
            } else {
                AgentOutcome.ToolChainExecuted(
                    steps = completedSteps.toList(),
                    finalMessage = NaturalLanguage(
                        "Stopped after ${ForemanChainLimits.MAX_SAFE_STEPS_PER_COMMAND} steps. " +
                            ForemanToolResultSummarizer.toUserSummary(completedSteps).value
                    )
                )
            },
            session = workingSession
        )
    }

    private suspend fun buildSavePreview(args: SaveInvoiceFromDraftArgs): InvoiceSavePreview? {
        val draft = draftRepository.getDraft().first()
        if (draft.clientName.isBlank() || draft.lineItems.isEmpty()) return null
        val subtotal = draft.lineItems.map { it.amount.value }.sum()
        val tax = subtotal * (draft.taxRate / 100.0)
        val total = subtotal + tax - draft.deposit
        return InvoiceSavePreview(
            clientName = NaturalLanguage(draft.clientName),
            lineItemCount = draft.lineItems.size,
            estimatedTotal = total.coerceAtLeast(0.0),
            isEstimate = args.isEstimate
        )
    }

    private suspend fun executeTool(toolName: ToolName, arguments: ToolArguments): ToolExecutionResult {
        return try {
            // Pillar 5: Secure Destructive Tools with Biometric Approval Gates
            if (ForemanToolPolicy.safetyFor(toolName) == ToolSafety.RequiresApproval) {
                if (platformActions.isBiometricAvailable()) {
                    val authenticated = withContext(kotlinx.coroutines.Dispatchers.Main) {
                        suspendCancellableCoroutine<Boolean> { continuation ->
                            platformActions.authenticateBiometric(
                                title = "Verify Identity",
                                subtitle = "Authentication required to authorize $toolName",
                                onSuccess = {
                                    if (continuation.isActive) continuation.resume(true)
                                },
                                onError = { _ ->
                                    if (continuation.isActive) continuation.resume(false)
                                }
                            )
                        }
                    }
                    if (!authenticated) {
                        return ToolExecutionResult.Failure(
                            toolName,
                            FailureMessage("Biometric authentication failed or was cancelled.")
                        )
                    }
                }
            }
            toolRegistry.execute(toolName, arguments)
        } catch (e: Throwable) {
            ToolExecutionResult.Failure(
                toolName,
                FailureMessage(e.message ?: "Tool execution failed.")
            )
        }
    }

    private fun recordToolStep(
        session: ForemanSession,
        request: AgentOutcome.ToolExecutionRequested,
        result: ToolExecutionResult,
        timestamp: TimestampMillis
    ): ForemanSession {
        val withTurn = appendToolTurn(session, request.toolCallId, request.toolName, result, timestamp)
        return ForemanSessionReducer.apply(withTurn, request.toolName, result, request.arguments)
    }

    private fun appendToolTurn(
        session: ForemanSession,
        toolCallId: com.fordham.toolbelt.domain.model.agent.ToolCallId,
        toolName: ToolName,
        result: ToolExecutionResult,
        timestamp: TimestampMillis
    ): ForemanSession {
        return session.append(
            ForemanTurn(
                role = AgentRole.ToolSystem,
                content = ForemanToolResultSummarizer.toContextLine(toolName, result),
                timestamp = timestamp,
                toolCallId = toolCallId,
                toolName = toolName
            )
        )
    }

    private fun validateToolRequest(
        toolName: ToolName,
        arguments: ToolArguments
    ): AgentOutcome.Failure? {
        return if (ToolArgumentValidator.isCompatible(toolName, arguments)) {
            null
        } else {
            AgentOutcome.Failure(
                FailureMessage("Tool arguments do not match requested tool.")
            )
        }
    }

    private fun latestUserCommand(session: ForemanSession): String {
        return session.history.lastOrNull { it.role == AgentRole.User }?.content?.value.orEmpty()
    }

    private suspend fun foremanAccessFailure(): AgentOutcome.Failure? {
        return when (val outcome = consumeToken(PremiumFeature.FOREMAN_AGENT)) {
            is TokenConsumptionOutcome.Success -> null
            is TokenConsumptionOutcome.InsufficientTokens ->
                AgentOutcome.Failure(
                    FailureMessage("Foreman needs Pro or Hammer credits. Open Settings → Subscription.")
                )
            is TokenConsumptionOutcome.Failure -> AgentOutcome.Failure(outcome.error)
        }
    }
}
