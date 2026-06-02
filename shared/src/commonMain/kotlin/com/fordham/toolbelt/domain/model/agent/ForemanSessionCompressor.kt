package com.fordham.toolbelt.domain.model.agent

/**
 * Deterministic session compression before LLM prompts — preserves actionable state only.
 */
object ForemanSessionCompressor {
    private const val MAX_SUMMARY_CHARS = 2000

    fun compressForPrompt(session: ForemanSession): ForemanSession {
        if (session.history.size <= ForemanChainLimits.MAX_HISTORY_TURNS_BEFORE_COMPRESS) {
            return session
        }
        val lastUserIdx = session.history.indexOfLast { it.role == AgentRole.User }
        if (lastUserIdx <= 0) return session

        val staleStart = if (session.history.firstOrNull()?.let(::isSummaryTurn) == true) 1 else 0
        val stale = session.history.subList(staleStart, lastUserIdx)
        val active = session.history.subList(lastUserIdx, session.history.size)
        if (stale.isEmpty()) return session

        val priorSummary = session.history.firstOrNull()
            ?.takeIf { isSummaryTurn(it) }
            ?.content
            ?.value

        val summaryTurn = ForemanTurn(
            role = AgentRole.ToolSystem,
            content = NaturalLanguage(buildSummary(stale, session, priorSummary)),
            timestamp = stale.last().timestamp
        )
        return session.copy(history = listOf(summaryTurn) + active)
    }

    private fun isSummaryTurn(turn: ForemanTurn): Boolean =
        turn.role == AgentRole.ToolSystem &&
            turn.content.value.startsWith(ForemanOperatingRules.SUMMARY_MARKER)

    private fun buildSummary(
        staleTurns: List<ForemanTurn>,
        session: ForemanSession,
        priorSummary: String?
    ): String = buildString {
        append(ForemanOperatingRules.SUMMARY_MARKER).append('\n')
        priorSummary?.let { prior ->
            append(prior.removePrefix(ForemanOperatingRules.SUMMARY_MARKER).trim())
            append('\n')
        }
        staleTurns.filter { it.role == AgentRole.User }.take(4).forEach { turn ->
            append("goal: ").append(turn.content.value.take(120)).append('\n')
        }
        session.activeClient?.let { append("active_client_id=").append(it.value).append('\n') }
        session.activeDraftInvoice?.let { append("active_draft_invoice_id=").append(it.value).append('\n') }
        session.resolvedEntities.entries().take(6).forEach { entry ->
            when (val entity = entry.entity) {
                is ResolvedClient ->
                    append("resolved: \"").append(entry.alias.value)
                        .append("\" -> client_id=").append(entity.id.value).append('\n')
                is ResolvedInvoice ->
                    append("resolved: \"").append(entry.alias.value)
                        .append("\" -> invoice_id=").append(entity.id.value).append('\n')
                is ResolvedReceipt ->
                    append("resolved: \"").append(entry.alias.value)
                        .append("\" -> receipt_id=").append(entity.id.value).append('\n')
            }
        }
        staleTurns.filter { it.role == AgentRole.ToolSystem && !isSummaryTurn(it) }.takeLast(8).forEach { turn ->
            append(turn.content.value).append('\n')
        }
    }.trim().take(MAX_SUMMARY_CHARS)
}
