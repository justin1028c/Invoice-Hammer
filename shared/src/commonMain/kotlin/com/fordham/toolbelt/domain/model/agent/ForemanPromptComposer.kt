package com.fordham.toolbelt.domain.model.agent

import com.fordham.toolbelt.domain.repository.ForemanJobMemoryPort

data class ForemanPlannerRequest(
    val userInput: String,
    val contextBlock: String,
    val systemInstruction: String,
    val toolCallingMode: ForemanToolCallingMode
)

/**
 * Single source for Foreman LLM planner prompts (operating rules + app snapshot + session).
 */
object ForemanPromptComposer {
    suspend fun compose(
        systemPrompt: NaturalLanguage,
        session: ForemanSession,
        jobMemory: ForemanJobMemoryPort
    ): ForemanPlannerRequest {
        val userInput = ForemanSessionPromptBuilder.buildChainedInput(session).value
        val runtime = ForemanRuntimeBinding.current()
        val lastUserCommand = session.history.lastOrNull { it.role == AgentRole.User }?.content?.value.orEmpty()
        val skill = AgentSkillClassifier.classify(lastUserCommand.ifBlank { userInput })

        val contextBlock = buildString {
            append(systemPrompt.value)
            append("\n\n[ACTIVE SKILL INTERACTION] Active skill selected: ")
            append(skill::class.simpleName)
            append("\nSkill specific rules: ").append(skill.systemInstruction)
            appendVoiceHypotheses(runtime)
            if (runtime.knownClientsCatalog.isNotBlank()) {
                append("\n\n").append(runtime.knownClientsCatalog)
            }
            if (runtime.knownSuppliersCatalog.isNotBlank()) {
                append("\n\n").append(runtime.knownSuppliersCatalog)
            }
            runtime.lastSavedInvoiceId?.let {
                append("\nLAST_SAVED_INVOICE_ID: ").append(it.value)
            }
            runtime.lastSavedInvoiceClientName?.takeIf { it.isNotBlank() }?.let {
                append("\nLAST_SAVED_INVOICE_CLIENT: ").append(it)
            }
            if (runtime.pendingReceiptImageBytes != null) {
                append("\nPENDING_RECEIPT_PHOTO: true")
            }
            if (hasToolStepsForCurrentCommand(session)) {
                append("\n\n[CHAIN MODE] Continue from COMPLETED STEPS. One tool per response unless done.")
            }
            append(ForemanSessionReducer.formatForContext(session))
        }

        val fullSystemInstruction = buildString {
            append(ForemanOperatingRules.core())
            append("\n\n[CRITICAL ACTIVE SKILL RULE]\n")
            append(skill.systemInstruction)
        }

        return ForemanPlannerRequest(
            userInput = userInput,
            contextBlock = jobMemory.appendRelevantNotes(contextBlock, userInput),
            systemInstruction = fullSystemInstruction,
            toolCallingMode = ForemanToolCallingPolicy.forSession(session)
        )
    }

    private fun StringBuilder.appendVoiceHypotheses(runtime: ForemanRuntimeSnapshot) {
        val meta = runtime.voiceTranscriptMeta ?: return
        if (meta.alternatives.isNotEmpty()) {
            append("\n\n[AUDIO HYPOTHESES]")
            meta.alternatives.take(3).forEach { append("\n- ").append(it) }
        }
        meta.confidence?.let { append("\nSTT_CONFIDENCE: ").append(it) }
    }

    private fun hasToolStepsForCurrentCommand(session: ForemanSession): Boolean {
        val lastUserIdx = session.history.indexOfLast { it.role == AgentRole.User }
        if (lastUserIdx < 0) return false
        return session.history.subList(lastUserIdx + 1, session.history.size)
            .any { it.role == AgentRole.ToolSystem }
    }
}

