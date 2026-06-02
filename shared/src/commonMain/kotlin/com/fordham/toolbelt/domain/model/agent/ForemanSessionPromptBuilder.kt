package com.fordham.toolbelt.domain.model.agent

/**
 * Builds the user-facing input string passed to the LLM on each chain iteration.
 */
object ForemanSessionPromptBuilder {
    fun buildChainedInput(session: ForemanSession): NaturalLanguage {
        val lastUserIndex = session.history.indexOfLast { it.role == AgentRole.User }
        if (lastUserIndex == -1) return NaturalLanguage("")

        val userCommand = session.history[lastUserIndex].content
        val summaryBlock = session.history.firstOrNull()
            ?.takeIf { turn ->
                turn.role == AgentRole.ToolSystem &&
                    turn.content.value.startsWith(ForemanOperatingRules.SUMMARY_MARKER)
            }
            ?.content
            ?.value

        val completed = session.history.subList(lastUserIndex + 1, session.history.size)
            .filter { it.role == AgentRole.ToolSystem }

        if (completed.isEmpty()) {
            return if (summaryBlock != null) {
                NaturalLanguage("$summaryBlock\n\n${userCommand.value}")
            } else {
                userCommand
            }
        }

        val log = completed.joinToString("\n") { it.content.value }

        return NaturalLanguage(
            buildString {
                if (summaryBlock != null) {
                    append(summaryBlock).append("\n\n")
                }
                append("USER REQUEST: ").append(userCommand.value).append('\n')
                append("\nCOMPLETED STEPS:\n").append(log).append('\n')
                append(
                    "\nPick the NEXT single tool. Use ids from completed steps. " +
                        "If fully done, respond with brief plain text only (no function)."
                )
            }
        )
    }
}
