package com.fordham.toolbelt.domain.model.agent

/**
 * Single mapper from tool execution to spoken + overlay copy.
 */
data class ForemanUserMessage(
    val spoken: NaturalLanguage,
    val stepLabels: List<String> = emptyList()
)

object ForemanUserMessageMapper {
    fun forChain(
        steps: List<ChainedToolStep>,
        finalMessage: NaturalLanguage? = null
    ): ForemanUserMessage {
        if (steps.isEmpty()) {
            return ForemanUserMessage(finalMessage ?: NaturalLanguage("Done."))
        }
        if (steps.size == 1) {
            return forStep(steps.first(), finalMessage)
        }
        finalMessage?.value?.takeIf { it.isNotBlank() }?.let {
            return ForemanUserMessage(
                spoken = NaturalLanguage(it),
                stepLabels = steps.map(::stepLabel)
            )
        }
        return ForemanUserMessage(
            spoken = NaturalLanguage("Done."),
            stepLabels = steps.map(::stepLabel)
        )
    }

    fun forStep(step: ChainedToolStep, finalMessage: NaturalLanguage? = null): ForemanUserMessage {
        val spoken = spokenForResult(step.toolName, step.result, finalMessage)
        return ForemanUserMessage(spoken = spoken)
    }

    private fun spokenForResult(
        toolName: ToolName,
        result: ToolExecutionResult,
        finalMessage: NaturalLanguage?
    ): NaturalLanguage = when (result) {
        is ToolExecutionResult.TabOpened ->
            ForemanToolResultSummarizer.tabOpenedUserMessage(result.tab)
        is ToolExecutionResult.Failure ->
            finalMessage?.takeIf { it.value.isNotBlank() }
                ?: ForemanToolResultSummarizer.retryExhaustedMessage(result.error)
        is ToolExecutionResult.QuickInvoiceCompleted ->
            NaturalLanguage("Invoice saved for ${result.clientName.value}.")
        is ToolExecutionResult.QuickClientAndInvoiceCompleted ->
            if (result.clientCreated) {
                NaturalLanguage("Created ${result.clientName.value} and saved the invoice.")
            } else {
                NaturalLanguage("Invoice saved for ${result.clientName.value}.")
            }
        is ToolExecutionResult.QuickInvoiceFromUnbilledCompleted ->
            NaturalLanguage("Invoiced ${result.receiptCount} receipt(s) for ${result.clientName.value}.")
        is ToolExecutionResult.InvoiceSavedFromDraft ->
            NaturalLanguage("Invoice saved for ${result.clientName.value}.")
        is ToolExecutionResult.ClientSelected ->
            NaturalLanguage("Opened ${result.displayName.value}.")
        is ToolExecutionResult.DuplicateAndEditCompleted ->
            NaturalLanguage(
                "Loaded last invoice for ${result.clientName.value} with ${result.lineItemCount} line(s). Edit on New Invoice."
            )
        is ToolExecutionResult.DraftInvoiceUpdated ->
            NaturalLanguage("Draft ready for ${result.clientName.value} with ${result.lineItemCount} line(s).")
        is ToolExecutionResult.OpenLastInvoiceCompleted ->
            NaturalLanguage("Opened invoice PDF.")
        is ToolExecutionResult.OpenSupplierCompleted ->
            NaturalLanguage("Opened ${result.name}.")
        else ->
            finalMessage?.takeIf { it.value.isNotBlank() } ?: NaturalLanguage("Done.")
    }

    private fun stepLabel(step: ChainedToolStep): String = when (val result = step.result) {
        is ToolExecutionResult.ClientSearchCompleted ->
            if (result.clients.isEmpty()) "Searched clients — none found"
            else "Searched clients — ${result.clients.size} match(es)"
        is ToolExecutionResult.ClientSelected -> "Selected ${result.displayName.value}"
        is ToolExecutionResult.ClientCreated -> "Created client ${result.displayName.value}"
        is ToolExecutionResult.DraftInvoiceCreated -> "Started draft invoice"
        is ToolExecutionResult.DraftInvoiceUpdated -> "Updated draft (${result.lineItemCount} lines)"
        is ToolExecutionResult.InvoiceSavedFromDraft -> "Saved invoice for ${result.clientName.value}"
        is ToolExecutionResult.QuickInvoiceCompleted -> "Saved invoice for ${result.clientName.value}"
        is ToolExecutionResult.QuickClientAndInvoiceCompleted ->
            if (result.clientCreated) "Created ${result.clientName.value} and saved invoice"
            else "Saved invoice for ${result.clientName.value}"
        is ToolExecutionResult.DuplicateAndEditCompleted ->
            "Duplicated last invoice for ${result.clientName.value}"
        is ToolExecutionResult.TabOpened -> "Opened ${result.tab.navLabel}"
        is ToolExecutionResult.Failure -> "Failed: ${result.error.value}"
        else -> ForemanToolResultSummarizer.toContextLine(step.toolName, result).value
            .substringAfter("→", missingDelimiterValue = "Step complete")
            .trim()
            .ifBlank { "Step complete" }
    }
}
