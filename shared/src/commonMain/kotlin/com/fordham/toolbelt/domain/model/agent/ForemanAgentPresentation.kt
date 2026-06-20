package com.fordham.toolbelt.domain.model.agent

import com.fordham.toolbelt.util.DateTimeUtil
import com.fordham.toolbelt.util.UserFacingCopy

object ForemanAgentPresentation {
    fun approvalMessage(pending: AgentOutcome.RequiresApproval): String = when (pending.toolName) {
        ToolName.SendInvoiceEmail -> UserFacingCopy.ForemanApproval.sendEmail()
        ToolName.SendInvoiceSms -> UserFacingCopy.ForemanApproval.sendSms()
        ToolName.QuickSendInvoice -> UserFacingCopy.ForemanApproval.quickSend()
        ToolName.DeleteInvoiceForApproval -> UserFacingCopy.ForemanApproval.deleteInvoice()
        ToolName.QuickInvoice -> {
            val args = pending.arguments as? QuickInvoiceArgs
            val total = DateTimeUtil.formatMoney(args?.lineItems?.sumOf { it.amount } ?: 0.0)
            UserFacingCopy.ForemanApproval.saveInvoice(total, args?.clientName?.value.orEmpty())
        }
        ToolName.QuickClientAndInvoice -> {
            val args = pending.arguments as? QuickClientAndInvoiceArgs
            val total = DateTimeUtil.formatMoney(args?.lineItems?.sumOf { it.amount } ?: 0.0)
            UserFacingCopy.ForemanApproval.createClientAndSave(total, args?.clientName?.value.orEmpty())
        }
        else -> UserFacingCopy.ForemanApproval.defaultPrompt()
    }

    fun stepsFromOutcome(outcome: AgentOutcome): List<ChainedToolStep> = when (outcome) {
        is AgentOutcome.ToolChainExecuted -> outcome.steps
        is AgentOutcome.RequiresApproval -> outcome.completedSteps
        is AgentOutcome.ClientChoiceRequired -> outcome.completedSteps
        is AgentOutcome.SaveConfirmationRequired -> outcome.completedSteps
        else -> emptyList()
    }
}
