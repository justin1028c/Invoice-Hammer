package com.fordham.toolbelt.domain.model.agent

enum class ForemanToolCallingMode(val apiValue: String) {
    RequireTool("ANY"),
    AllowCompletionText("AUTO")
}

object ForemanToolCallingPolicy {
    private const val AUTO_AFTER_TOOL_STEPS = 1

    fun forSession(session: ForemanSession): ForemanToolCallingMode {
        val lastUserIdx = session.history.indexOfLast { it.role == AgentRole.User }
        if (lastUserIdx < 0) return ForemanToolCallingMode.RequireTool
        
        val lastCommand = session.history[lastUserIdx].content.value
        if (AgentSkillClassifier.classify(lastCommand) is AgentSkill.HelpSkills) {
            return ForemanToolCallingMode.AllowCompletionText
        }

        if (lastUserIdx > 0 && session.history[lastUserIdx - 1].role == AgentRole.Foreman) {
            return ForemanToolCallingMode.AllowCompletionText
        }

        val toolStepCount = session.history
            .subList(lastUserIdx + 1, session.history.size)
            .count { it.role == AgentRole.ToolSystem }
        return if (toolStepCount >= AUTO_AFTER_TOOL_STEPS) {
            ForemanToolCallingMode.AllowCompletionText
        } else {
            ForemanToolCallingMode.RequireTool
        }
    }
}
