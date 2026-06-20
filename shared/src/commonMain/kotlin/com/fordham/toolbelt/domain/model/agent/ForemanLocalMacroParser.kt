package com.fordham.toolbelt.domain.model.agent

/**
 * High-confidence local macro detection — skips Gemini when utterance clearly maps to one tool.
 */
object ForemanLocalMacroParser {
    private val UNBILLED = Regex(
        """^(?:bill|invoice)\s+(?:all\s+)?(?:the\s+)?unbilled(?:\s+receipts?)?(?:\s+for|\s+to|\s+from)?\s+(.+)$""",
        RegexOption.IGNORE_CASE
    )

    private val DUPLICATE = Regex(
        """^(?:duplicate|repeat|copy|redo)\s+(?:the\s+)?last\s+invoice(?:\s+for|\s+to|\s+of)?\s+(.+)$""",
        RegexOption.IGNORE_CASE
    )

    private val SAME_AS_LAST = Regex(
        """^same as last(?: time)?(?:\s+for|\s+to)?\s+(.+)$""",
        RegexOption.IGNORE_CASE
    )

    private val DUPLICATE_AND_EDIT = Regex(
        """^(?:same as last(?: time)?|duplicate(?: last invoice)?)\s+(?:for|to)\s+(.+?)\s+(?:but|and)\s+add\s+(.+)$""",
        RegexOption.IGNORE_CASE
    )

    private val NEW_CLIENT_INVOICE = Regex(
        """^(?:new client|add client)\s+(.+?)\s+(?:and\s+)?(?:invoice|bill|charge)\s+(.+)$""",
        RegexOption.IGNORE_CASE
    )

    private val QUICK_INVOICE_VERB = Regex(
        """^(?:invoice|bill|charge)\s+(.+)$""",
        RegexOption.IGNORE_CASE
    )

    private val HOURS_AT_RATE = Regex(
        """^(.+?)\s+(\d+(?:\.\d+)?)\s*(?:hours?|hrs?|h)\s+at\s+\$?(\d+(?:\.\d+)?)(?:\s+(?:an?\s+)?hour)?(?:\s+(?:for\s+)?(.+))?$""",
        RegexOption.IGNORE_CASE
    )

    private val FLAT_AMOUNT = Regex(
        """^(.+?)\s+(?:for\s+)?\$?(\d+(?:\.\d{1,2})?)\s*(?:dollars?|bucks?)?(?:\s+(?:for\s+)?(.+))?$""",
        RegexOption.IGNORE_CASE
    )

    private val ADD_LINE_AMOUNT = Regex(
        """^\$?(\d+(?:\.\d{1,2})?)\s+(?:for\s+)?(.+)$""",
        RegexOption.IGNORE_CASE
    )

    private val AMOUNT_INDICATOR = Regex(
        """(?:\$|\d+(?:\.\d{1,2})?\s*(?:dollars?|bucks?)|\d+(?:\.\d{1,2})?\s+for\s|\d+\s*(?:hours?|hrs?|h)\s+at\s+\$?\d+)""",
        RegexOption.IGNORE_CASE
    )

    private val CONTEXTUAL_HOURS_AT_RATE = Regex(
        """^(?:add\s+)?labor,?\s*(\d+(?:\.\d+)?)\s*(?:hours?|hrs?|h)?\s+at\s+\$?(\d+(?:\.\d+)?)(?:\s*(?:dollars?|bucks?))?(?:\s+(?:an?\s+)?hour)?(?:\s+(?:for\s+)?(.+))?$""",
        RegexOption.IGNORE_CASE
    )

    private val CONTEXTUAL_FLAT_AMOUNT = Regex(
        """^(?:add\s+)?(?:service|materials|drywall|painting|carpentry|flooring|roofing|electrical|plumbing|job|charge),?\s+\$?(\d+(?:\.\d{1,2})?)\s*(?:dollars?|bucks?)?(?:\s+(?:for\s+)?(.+))?$""",
        RegexOption.IGNORE_CASE
    )

    private val CONTEXTUAL_FLAT_AMOUNT_REVERSE = Regex(
        """^(?:add\s+)?(?:for\s+)?(.+?)\s+,?\s*\$?(\d+(?:\.\d{1,2})?)\s*(?:dollars?|bucks?)?$""",
        RegexOption.IGNORE_CASE
    )

    fun parse(command: String): ForemanRoute.LocalMacro? {
        val text = command.trim()
        if (text.isBlank()) return null

        val activeTab = ForemanRuntimeBinding.current().activeTab
        if (activeTab == AppTab.NewInvoice) {
            // A. Send / Save invoice
            if (text.equals("send this invoice", ignoreCase = true) ||
                text.equals("send invoice", ignoreCase = true) ||
                text.equals("send it", ignoreCase = true)
            ) {
                return ForemanRoute.LocalMacro(
                    toolName = ToolName.SaveInvoiceFromDraft,
                    arguments = SaveInvoiceFromDraftArgs(
                        isEstimate = false,
                        autoShare = true
                    )
                )
            }

            // B. Attach receipt / scan receipt
            if (text.equals("attach receipt", ignoreCase = true) ||
                text.equals("attach last receipt", ignoreCase = true) ||
                text.equals("scan last receipt", ignoreCase = true) ||
                text.equals("scan receipt", ignoreCase = true)
            ) {
                val clientName = ForemanRuntimeBinding.current().selectedClientName?.let { com.fordham.toolbelt.domain.model.ClientName(it) }
                return ForemanRoute.LocalMacro(
                    toolName = ToolName.ScanLastReceipt,
                    arguments = ScanLastReceiptArgs(clientName = clientName)
                )
            }

            // C. Add labor hourly
            CONTEXTUAL_HOURS_AT_RATE.matchEntire(text)?.let { match ->
                val quantity = match.groupValues[1].toDoubleOrNull() ?: 1.0
                val unitPrice = match.groupValues[2].toDoubleOrNull() ?: 0.0
                val description = match.groupValues.getOrNull(3)?.trim()?.takeIf { it.isNotBlank() } ?: "Labor"
                return ForemanRoute.LocalMacro(
                    toolName = ToolName.AppendDraftLines,
                    arguments = AppendDraftLinesArgs(
                        lineItems = listOf(
                            DraftLineItemInput(
                                description = NaturalLanguage(description),
                                amount = quantity * unitPrice,
                                category = NaturalLanguage("Labor"),
                                quantity = quantity,
                                unitPrice = unitPrice
                            )
                        )
                    )
                )
            }

            // D. Add flat amount
            CONTEXTUAL_FLAT_AMOUNT.matchEntire(text)?.let { match ->
                val amount = match.groupValues[1].toDoubleOrNull() ?: 0.0
                val description = match.groupValues.getOrNull(2)?.trim()?.takeIf { it.isNotBlank() } ?: "Service"
                val category = if (text.contains("materials", ignoreCase = true)) "Materials" else "Service"
                return ForemanRoute.LocalMacro(
                    toolName = ToolName.AppendDraftLines,
                    arguments = AppendDraftLinesArgs(
                        lineItems = listOf(
                            DraftLineItemInput(
                                description = NaturalLanguage(description),
                                amount = amount,
                                category = NaturalLanguage(category),
                                quantity = 1.0,
                                unitPrice = amount
                            )
                        )
                    )
                )
            }

            // E. Add flat amount reverse
            CONTEXTUAL_FLAT_AMOUNT_REVERSE.matchEntire(text)?.let { match ->
                val description = match.groupValues[1].trim()
                val amount = match.groupValues[2].toDoubleOrNull() ?: 0.0
                if (description.isNotBlank() && !description.contains("tab", ignoreCase = true) && !description.contains("screen", ignoreCase = true)) {
                    return ForemanRoute.LocalMacro(
                        toolName = ToolName.AppendDraftLines,
                        arguments = AppendDraftLinesArgs(
                            lineItems = listOf(
                                DraftLineItemInput(
                                    description = NaturalLanguage(description),
                                    amount = amount,
                                    category = NaturalLanguage("Service"),
                                    quantity = 1.0,
                                    unitPrice = amount
                                )
                            )
                        )
                    )
                }
            }
        }

        // If the command is a complex dictation (contains phone, email, new client, or is long),
        // bypass local macros and route directly to the Gemini LLM for high-accuracy structured extraction.
        if (text.contains("phone", ignoreCase = true) || 
            text.contains("email", ignoreCase = true) ||
            text.length > 140
        ) {
            return null
        }

        UNBILLED.matchEntire(text)?.let { match ->
            val client = cleanClientName(match.groupValues[1]) ?: return null
            return ForemanRoute.LocalMacro(
                toolName = ToolName.QuickInvoiceFromUnbilledReceipts,
                arguments = QuickInvoiceFromUnbilledReceiptsArgs(
                    clientName = NaturalLanguage(client)
                )
            )
        }

        DUPLICATE_AND_EDIT.matchEntire(text)?.let { match ->
            val client = cleanClientName(match.groupValues[1]) ?: return null
            val lineItem = parseAdditionalLineItem(match.groupValues[2]) ?: return null
            return ForemanRoute.LocalMacro(
                toolName = ToolName.DuplicateAndEdit,
                arguments = DuplicateAndEditArgs(
                    clientName = NaturalLanguage(client),
                    additionalLineItems = listOf(lineItem)
                )
            )
        }

        DUPLICATE.matchEntire(text)?.let { match ->
            val client = cleanClientName(match.groupValues[1]) ?: return null
            return duplicateMacro(client)
        }

        SAME_AS_LAST.matchEntire(text)?.let { match ->
            val client = cleanClientName(match.groupValues[1]) ?: return null
            return duplicateMacro(client)
        }

        NEW_CLIENT_INVOICE.matchEntire(text)?.let { match ->
            val client = cleanClientName(match.groupValues[1]) ?: return null
            val invoiceBody = match.groupValues[2].trim()
            return parseQuickClientAndInvoice(client, invoiceBody)
        }

        return parseQuickInvoice(text)
    }

    private fun duplicateMacro(clientName: String): ForemanRoute.LocalMacro =
        ForemanRoute.LocalMacro(
            toolName = ToolName.DuplicateLastInvoice,
            arguments = DuplicateLastInvoiceArgs(
                clientName = NaturalLanguage(clientName)
            )
        )

    private fun parseQuickClientAndInvoice(clientName: String, invoiceBody: String): ForemanRoute.LocalMacro? {
        if (!AMOUNT_INDICATOR.containsMatchIn(invoiceBody)) return null
        val lineItems = parseInvoiceBodyLineItems(invoiceBody) ?: return null
        return ForemanRoute.LocalMacro(
            toolName = ToolName.QuickClientAndInvoice,
            arguments = QuickClientAndInvoiceArgs(
                clientName = NaturalLanguage(clientName),
                lineItems = lineItems
            )
        )
    }

    private fun parseInvoiceBodyLineItems(invoiceBody: String): List<DraftLineItemInput>? {
        val body = invoiceBody.trim()
        Regex(
            """^(\d+(?:\.\d+)?)\s*(?:hours?|hrs?|h)\s+at\s+\$?(\d+(?:\.\d+)?)(?:\s+(?:for\s+)?(.+))?$""",
            RegexOption.IGNORE_CASE
        ).matchEntire(body)?.let { match ->
            val quantity = match.groupValues[1].toDoubleOrNull() ?: return null
            val unitPrice = match.groupValues[2].toDoubleOrNull() ?: return null
            val description = match.groupValues.getOrNull(3)?.trim()?.takeIf { it.isNotBlank() } ?: "Labor"
            return listOf(
                DraftLineItemInput(
                    description = NaturalLanguage(description),
                    amount = quantity * unitPrice,
                    category = NaturalLanguage("Labor"),
                    quantity = quantity,
                    unitPrice = unitPrice
                )
            )
        }
        ADD_LINE_AMOUNT.matchEntire(body)?.let { match ->
            val amount = match.groupValues[1].toDoubleOrNull() ?: return null
            val description = match.groupValues[2].trim().ifBlank { "Service" }
            return listOf(
                DraftLineItemInput(
                    description = NaturalLanguage(description),
                    amount = amount,
                    category = NaturalLanguage("Service"),
                    quantity = 1.0,
                    unitPrice = amount
                )
            )
        }
        return null
    }

    private fun parseQuickInvoice(text: String): ForemanRoute.LocalMacro? {
        if (!AMOUNT_INDICATOR.containsMatchIn(text)) return null
        val verbMatch = QUICK_INVOICE_VERB.matchEntire(text) ?: return null
        val remainder = verbMatch.groupValues[1].trim()
        if (remainder.isBlank()) return null

        HOURS_AT_RATE.matchEntire(remainder)?.let { match ->
            val client = cleanClientName(match.groupValues[1]) ?: return null
            val quantity = match.groupValues[2].toDoubleOrNull() ?: return null
            val unitPrice = match.groupValues[3].toDoubleOrNull() ?: return null
            val description = match.groupValues.getOrNull(4)?.trim()?.takeIf { it.isNotBlank() } ?: "Labor"
            return quickInvoiceMacro(
                clientName = client,
                lineItems = listOf(
                    DraftLineItemInput(
                        description = NaturalLanguage(description),
                        amount = quantity * unitPrice,
                        category = NaturalLanguage("Labor"),
                        quantity = quantity,
                        unitPrice = unitPrice
                    )
                )
            )
        }

        FLAT_AMOUNT.matchEntire(remainder)?.let { match ->
            val client = cleanClientName(match.groupValues[1]) ?: return null
            val amount = match.groupValues[2].toDoubleOrNull() ?: return null
            val description = match.groupValues.getOrNull(3)?.trim()?.takeIf { it.isNotBlank() } ?: "Service"
            return quickInvoiceMacro(
                clientName = client,
                lineItems = listOf(
                    DraftLineItemInput(
                        description = NaturalLanguage(description),
                        amount = amount,
                        category = NaturalLanguage("Service"),
                        quantity = 1.0,
                        unitPrice = amount
                    )
                )
            )
        }

        return null
    }

    private fun parseLineItemsFromRemainder(remainder: String): List<DraftLineItemInput>? {
        HOURS_AT_RATE.matchEntire(remainder)?.let { match ->
            val quantity = match.groupValues[2].toDoubleOrNull() ?: return null
            val unitPrice = match.groupValues[3].toDoubleOrNull() ?: return null
            val description = match.groupValues.getOrNull(4)?.trim()?.takeIf { it.isNotBlank() } ?: "Labor"
            return listOf(
                DraftLineItemInput(
                    description = NaturalLanguage(description),
                    amount = quantity * unitPrice,
                    category = NaturalLanguage("Labor"),
                    quantity = quantity,
                    unitPrice = unitPrice
                )
            )
        }

        FLAT_AMOUNT.matchEntire(remainder)?.let { match ->
            val amount = match.groupValues[2].toDoubleOrNull() ?: return null
            val description = match.groupValues.getOrNull(3)?.trim()?.takeIf { it.isNotBlank() } ?: "Service"
            return listOf(
                DraftLineItemInput(
                    description = NaturalLanguage(description),
                    amount = amount,
                    category = NaturalLanguage("Service"),
                    quantity = 1.0,
                    unitPrice = amount
                )
            )
        }
        return null
    }

    private fun parseAdditionalLineItem(raw: String): DraftLineItemInput? {
        val text = raw.trim()
        ADD_LINE_AMOUNT.matchEntire(text)?.let { match ->
            val amount = match.groupValues[1].toDoubleOrNull() ?: return null
            val description = match.groupValues[2].trim().ifBlank { "Service" }
            return DraftLineItemInput(
                description = NaturalLanguage(description),
                amount = amount,
                category = NaturalLanguage("Service"),
                quantity = 1.0,
                unitPrice = amount
            )
        }
        return parseLineItemsFromRemainder(text)?.firstOrNull()
    }

    private fun quickInvoiceMacro(
        clientName: String,
        lineItems: List<DraftLineItemInput>
    ): ForemanRoute.LocalMacro {
        return ForemanRoute.LocalMacro(
            toolName = ToolName.QuickInvoice,
            arguments = QuickInvoiceArgs(
                clientName = NaturalLanguage(clientName),
                lineItems = lineItems
            )
        )
    }

    private fun cleanClientName(raw: String): String? {
        val cleaned = raw.trim()
            .replace(Regex("""^[.!?\s]+"""), "")
            .replace(Regex("""[.!?]+$"""), "")
            .replace(Regex("""\s+please$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""^to\s+""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""^for\s+""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""^from\s+""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+for$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+to$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+from$""", RegexOption.IGNORE_CASE), "")
            .trim()
        return cleaned.takeIf { it.length >= 2 }
    }
}
