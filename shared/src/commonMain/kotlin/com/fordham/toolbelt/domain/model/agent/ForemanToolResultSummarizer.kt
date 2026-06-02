package com.fordham.toolbelt.domain.model.agent

/**
 * Compact, LLM-facing summaries of tool results for multi-step agent chaining.
 */
object ForemanToolResultSummarizer {
    fun toContextLine(toolName: ToolName, result: ToolExecutionResult): NaturalLanguage {
        val line = when (result) {
            is ToolExecutionResult.ClientSearchCompleted ->
                if (result.clients.isEmpty()) "No clients found."
                else "Found ${result.clients.size} client(s):\n" +
                    result.clients.take(5).joinToString("\n") { 
                        "- ${it.displayName.value} (id=${it.clientId.value}): " +
                        "Owed: $${(it.totalOwed * 100).toInt() / 100.0}, " +
                        "Total Invoiced: $${(it.totalInvoiced * 100).toInt() / 100.0}, " +
                        "Invoices count: ${it.invoiceCount}, " +
                        "Address: ${it.address.ifBlank { "n/a" }}"
                    }
            is ToolExecutionResult.ClientSelected ->
                "Selected ${result.displayName.value} (id=${result.clientId.value})."
            is ToolExecutionResult.ClientDetailsLoaded -> result.summary.value
            is ToolExecutionResult.UnbilledReceiptsFound ->
                "Found ${result.receipts.size} unbilled receipt(s)."
            is ToolExecutionResult.TabOpened -> "TAB:${result.tab.navLabel}"
            is ToolExecutionResult.DraftInvoiceCreated ->
                "Draft invoice started for client id ${result.clientId.value}."
            is ToolExecutionResult.DraftInvoiceUpdated ->
                "Draft updated: ${result.clientName.value}, ${result.lineItemCount} line item(s)."
            is ToolExecutionResult.JobNoteAdded -> "Job note saved for ${result.clientName.value}."
            is ToolExecutionResult.InvoiceSavedFromDraft ->
                "Invoice saved: id=${result.invoiceId.value}, client=${result.clientName.value}, total≈${
                    (result.totalAmount * 100).toInt() / 100.0
                }, pdf=${result.pdfPath}."
            is ToolExecutionResult.ClientCreated ->
                "Client created: ${result.displayName.value} (id=${result.clientId.value})."
            is ToolExecutionResult.ReceiptScanned ->
                "Receipt scanned: ${result.itemCount} line item(s) added."
            is ToolExecutionResult.QuickInvoiceCompleted ->
                "Quick invoice saved for ${result.clientName.value} (id=${result.invoiceId.value}, total≈${
                    (result.totalAmount * 100).toInt() / 100.0
                })."
            is ToolExecutionResult.QuickClientAndInvoiceCompleted ->
                "Quick client+invoice saved for ${result.clientName.value}" +
                    (if (result.clientCreated) " (client created)" else "") +
                    " (id=${result.invoiceId.value}, total≈${(result.totalAmount * 100).toInt() / 100.0})."
            is ToolExecutionResult.DuplicateAndEditCompleted ->
                "Duplicated last invoice for ${result.clientName.value} with ${result.lineItemCount} line(s) in draft."
            is ToolExecutionResult.QuickInvoiceFromUnbilledCompleted ->
                "Invoiced ${result.receiptCount} unbilled receipt(s) for ${result.clientName.value} " +
                    "(id=${result.invoiceId.value}, total≈${(result.totalAmount * 100).toInt() / 100.0})."
            is ToolExecutionResult.QuickClientLookupCompleted -> result.summary.value
            is ToolExecutionResult.InvoiceHistorySearched -> {
                val detail = result.summary.value.takeIf { it.isNotBlank() }
                    ?: "No results."
                "History search '${result.query.value}': $detail"
            }
            is ToolExecutionResult.InvoiceSendQueued ->
                "Send queued via ${result.channel.value} for invoice ${result.invoiceId.value}."
            is ToolExecutionResult.InvoiceDeletionQueued ->
                "Invoice ${result.invoiceId.value} deleted."
            is ToolExecutionResult.OpenLastInvoiceCompleted ->
                "Opened invoice PDF: ${result.pdfPath}."
            is ToolExecutionResult.OpenSupplierCompleted ->
                "Opened supplier store portal: ${result.name}."
            is ToolExecutionResult.Failure -> "FAILED: ${result.error.value}"
        }
        return NaturalLanguage("${toolLabel(toolName)} → $line")
    }

    fun tabOpenedUserMessage(tab: AppTab): NaturalLanguage =
        NaturalLanguage("Opened ${tab.navLabel}.")

    fun retryExhaustedMessage(error: com.fordham.toolbelt.domain.model.FailureMessage): NaturalLanguage =
        NaturalLanguage("${error.value} Tell me what to change or try a different action.")

    fun toUserSummary(steps: List<ChainedToolStep>, finalMessage: NaturalLanguage? = null): NaturalLanguage =
        ForemanUserMessageMapper.forChain(steps, finalMessage).spoken

    private fun briefUserMessage(step: ChainedToolStep, finalMessage: NaturalLanguage?): NaturalLanguage =
        ForemanUserMessageMapper.forStep(step, finalMessage).spoken

    private fun toolLabel(toolName: ToolName): String = when (toolName) {
        ToolName.SearchClients -> "SEARCH_CLIENTS"
        ToolName.SelectClient -> "SELECT_CLIENT"
        ToolName.GetClientDetails -> "GET_CLIENT_DETAILS"
        ToolName.GetUnbilledReceipts -> "GET_UNBILLED_RECEIPTS"
        ToolName.OpenTab -> "OPEN_TAB"
        ToolName.CreateDraftInvoice -> "CREATE_DRAFT_INVOICE"
        ToolName.UpdateDraftInvoice -> "UPDATE_DRAFT_INVOICE"
        ToolName.AddJobNote -> "ADD_JOB_NOTE"
        ToolName.SaveInvoiceFromDraft -> "SAVE_INVOICE"
        ToolName.SearchInvoiceHistory -> "SEARCH_INVOICE_HISTORY"
        ToolName.CreateClient -> "CREATE_CLIENT"
        ToolName.ScanLastReceipt -> "SCAN_LAST_RECEIPT"
        ToolName.QuickInvoice -> "QUICK_INVOICE"
        ToolName.QuickClientAndInvoice -> "QUICK_CLIENT_AND_INVOICE"
        ToolName.QuickClientLookup -> "QUICK_CLIENT_LOOKUP"
        ToolName.AppendDraftLines -> "APPEND_DRAFT_LINES"
        ToolName.DuplicateLastInvoice -> "DUPLICATE_LAST_INVOICE"
        ToolName.DuplicateAndEdit -> "DUPLICATE_AND_EDIT"
        ToolName.QuickInvoiceFromUnbilledReceipts -> "QUICK_INVOICE_FROM_UNBILLED_RECEIPTS"
        ToolName.QuickSendInvoice -> "QUICK_SEND_INVOICE"
        ToolName.SendInvoiceEmail -> "SEND_INVOICE_EMAIL"
        ToolName.SendInvoiceSms -> "SEND_INVOICE_SMS"
        ToolName.DeleteInvoiceForApproval -> "DELETE_INVOICE"
        ToolName.OpenLastInvoice -> "OPEN_LAST_INVOICE"
        ToolName.OpenSupplier -> "OPEN_SUPPLIER"
    }
}
