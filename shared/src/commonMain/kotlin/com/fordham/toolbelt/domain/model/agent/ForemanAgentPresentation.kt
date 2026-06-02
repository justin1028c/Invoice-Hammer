package com.fordham.toolbelt.domain.model.agent

object ForemanAgentPresentation {
    fun approvalMessage(pending: AgentOutcome.RequiresApproval): String = when (pending.toolName) {
        ToolName.SendInvoiceEmail -> "Approve sending this invoice by email?"
        ToolName.SendInvoiceSms -> "Approve sending this invoice by text message?"
        ToolName.QuickSendInvoice -> "Approve sending the latest invoice?"
        ToolName.DeleteInvoiceForApproval -> "Approve deleting this invoice?"
        ToolName.QuickInvoice -> {
            val args = pending.arguments as? QuickInvoiceArgs
            val total = args?.lineItems?.sumOf { it.amount } ?: 0.0
            "Approve saving $${(total * 100).toInt() / 100.0} invoice for ${args?.clientName?.value}?"
        }
        ToolName.QuickClientAndInvoice -> {
            val args = pending.arguments as? QuickClientAndInvoiceArgs
            val total = args?.lineItems?.sumOf { it.amount } ?: 0.0
            "Approve creating client and saving $${(total * 100).toInt() / 100.0} invoice for ${args?.clientName?.value}?"
        }
        else -> "This action needs your approval."
    }

    fun stepsFromOutcome(outcome: AgentOutcome): List<ChainedToolStep> = when (outcome) {
        is AgentOutcome.ToolChainExecuted -> outcome.steps
        is AgentOutcome.RequiresApproval -> outcome.completedSteps
        is AgentOutcome.ClientChoiceRequired -> outcome.completedSteps
        is AgentOutcome.SaveConfirmationRequired -> outcome.completedSteps
        else -> emptyList()
    }
}
