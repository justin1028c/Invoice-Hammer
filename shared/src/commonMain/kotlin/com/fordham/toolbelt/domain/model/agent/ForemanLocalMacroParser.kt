package com.fordham.toolbelt.domain.model.agent

import com.fordham.toolbelt.util.AppLocale

/**
 * High-confidence local macro detection — skips Gemini when utterance clearly maps to one tool.
 *
 * FIX (Spanish voice-routing gap): every pattern below now has a Spanish counterpart tried
 * alongside the English one, so Spanish contractors get the same "skip the network round-trip"
 * fast path English contractors already had. Replaces the old single rigid template
 * ("Factura a X Y por Z") with real coverage of quick-invoice, hours-at-rate, flat-amount,
 * duplicate-last-invoice, and new-client-invoice — the same macro set English supports.
 *
 * The old ParseVoiceInvoiceDeterministicallyUseCase.parseSimpleFactura() template still works
 * (it's a superset case) but is no longer the only Spanish path.
 */
object ForemanLocalMacroParser {
    // ---------- English (unchanged) ----------
    private val UNBILLED_EN = Regex(
        """^(?:bill|invoice)\s+(?:all\s+)?(?:the\s+)?unbilled(?:\s+receipts?)?(?:\s+for|\s+to|\s+from)?\s+(.+)$""",
        RegexOption.IGNORE_CASE
    )

    private val DUPLICATE_EN = Regex(
        """^(?:duplicate|repeat|copy|redo)\s+(?:the\s+)?last\s+invoice(?:\s+for|\s+to|\s+of)?\s+(.+)$""",
        RegexOption.IGNORE_CASE
    )

    private val SAME_AS_LAST_EN = Regex(
        """^same as last(?: time)?(?:\s+for|\s+to)?\s+(.+)$""",
        RegexOption.IGNORE_CASE
    )

    private val DUPLICATE_AND_EDIT_EN = Regex(
        """^(?:same as last(?: time)?|duplicate(?: last invoice)?)\s+(?:for|to)\s+(.+?)\s+(?:but|and)\s+add\s+(.+)$""",
        RegexOption.IGNORE_CASE
    )

    private val NEW_CLIENT_INVOICE_EN = Regex(
        """^(?:new client|add client)\s+(.+?)\s+(?:and\s+)?(?:invoice|bill|charge)\s+(.+)$""",
        RegexOption.IGNORE_CASE
    )

    private val QUICK_INVOICE_VERB_EN = Regex(
        """^(?:invoice|bill|charge)\s+(.+)$""",
        RegexOption.IGNORE_CASE
    )

    private val HOURS_AT_RATE_EN = Regex(
        """^(.+?)\s+(\d+(?:\.\d+)?)\s*(?:hours?|hrs?|h)\s+at\s+\$?(\d+(?:\.\d+)?)(?:\s+(?:an?\s+)?hour)?(?:\s+(?:for\s+)?(.+))?$""",
        RegexOption.IGNORE_CASE
    )

    private val FLAT_AMOUNT_EN = Regex(
        """^(.+?)\s+(?:for\s+)?\$?(\d+(?:\.\d{1,2})?)\s*(?:dollars?|bucks?)?(?:\s+(?:for\s+)?(.+))?$""",
        RegexOption.IGNORE_CASE
    )

    private val ADD_LINE_AMOUNT_EN = Regex(
        """^\$?(\d+(?:\.\d{1,2})?)\s+(?:for\s+)?(.+)$""",
        RegexOption.IGNORE_CASE
    )

    private val AMOUNT_INDICATOR_EN = Regex(
        """(?:\$|\d+(?:\.\d{1,2})?\s*(?:dollars?|bucks?)|\d+(?:\.\d{1,2})?\s+for\s|\d+\s*(?:hours?|hrs?|h)\s+at\s+\$?\d+)""",
        RegexOption.IGNORE_CASE
    )

    private val CONTEXTUAL_HOURS_AT_RATE_EN = Regex(
        """^(?:add\s+)?labor,?\s*(\d+(?:\.\d+)?)\s*(?:hours?|hrs?|h)?\s+at\s+\$?(\d+(?:\.\d+)?)(?:\s*(?:dollars?|bucks?))?(?:\s+(?:an?\s+)?hour)?(?:\s+(?:for\s+)?(.+))?$""",
        RegexOption.IGNORE_CASE
    )

    private val CONTEXTUAL_FLAT_AMOUNT_EN = Regex(
        """^(?:add\s+)?(?:service|materials|drywall|painting|carpentry|flooring|roofing|electrical|plumbing|job|charge),?\s+\$?(\d+(?:\.\d{1,2})?)\s*(?:dollars?|bucks?)?(?:\s+(?:for\s+)?(.+))?$""",
        RegexOption.IGNORE_CASE
    )

    private val CONTEXTUAL_FLAT_AMOUNT_REVERSE_EN = Regex(
        """^(?:add\s+)?(?:for\s+)?(.+?)\s+,?\s*\$?(\d+(?:\.\d{1,2})?)\s*(?:dollars?|bucks?)?$""",
        RegexOption.IGNORE_CASE
    )

    // ---------- Spanish (new) ----------
    private val UNBILLED_ES = Regex(
        """^(?:factura|cobra)\s+(?:todos\s+)?(?:los\s+)?recibos?\s+(?:sin\s+facturar|pendientes)(?:\s+(?:de|para))?\s+(.+)$""",
        RegexOption.IGNORE_CASE
    )

    private val DUPLICATE_ES = Regex(
        """^(?:duplica|repite|copia|rehaz)\s+(?:la\s+)?última\s+factura(?:\s+(?:de|para))?\s+(.+)$""",
        RegexOption.IGNORE_CASE
    )

    private val SAME_AS_LAST_ES = Regex(
        """^igual\s+que\s+la\s+última(?:\s+vez)?(?:\s+(?:de|para))?\s+(.+)$""",
        RegexOption.IGNORE_CASE
    )

    private val DUPLICATE_AND_EDIT_ES = Regex(
        """^(?:igual\s+que\s+la\s+última(?:\s+vez)?|duplica(?:\s+la\s+última\s+factura)?)\s+(?:de|para)\s+(.+?)\s+(?:pero|y)\s+agrega\s+(.+)$""",
        RegexOption.IGNORE_CASE
    )

    private val NEW_CLIENT_INVOICE_ES = Regex(
        """^(?:nuevo\s+cliente|agrega\s+cliente|añade\s+cliente)\s+(.+?)\s+(?:y\s+)?(?:factura|cobra)\s+(.+)$""",
        RegexOption.IGNORE_CASE
    )

    private val QUICK_INVOICE_VERB_ES = Regex(
        """^(?:factura|facturale|factúrale|cobrale|cóbrale|cobra)\s+(?:a\s+)?(.+)$""",
        RegexOption.IGNORE_CASE
    )

    private val HOURS_AT_RATE_ES = Regex(
        """^(.+?)\s+(\d+(?:\.\d+)?)\s*(?:horas?|hrs?|h)\s+a\s+\$?(\d+(?:\.\d+)?)(?:\s+la\s+hora)?(?:\s+(?:por\s+)?(.+))?$""",
        RegexOption.IGNORE_CASE
    )

    private val FLAT_AMOUNT_ES = Regex(
        """^(.+?)\s+(?:por\s+)?\$?(\d+(?:\.\d{1,2})?)\s*(?:dólares?|dolares?|pesos?)?(?:\s+(?:por\s+)?(.+))?$""",
        RegexOption.IGNORE_CASE
    )

    private val ADD_LINE_AMOUNT_ES = Regex(
        """^\$?(\d+(?:\.\d{1,2})?)\s+(?:por\s+)?(.+)$""",
        RegexOption.IGNORE_CASE
    )

    private val AMOUNT_INDICATOR_ES = Regex(
        """(?:\$|\d+(?:\.\d{1,2})?\s*(?:dólares?|dolares?|pesos?)|\d+(?:\.\d{1,2})?\s+por\s|\d+\s*(?:horas?|hrs?|h)\s+a\s+\$?\d+)""",
        RegexOption.IGNORE_CASE
    )

    private val CONTEXTUAL_HOURS_AT_RATE_ES = Regex(
        """^(?:agrega\s+)?mano\s+de\s+obra,?\s*(\d+(?:\.\d+)?)\s*(?:horas?|hrs?|h)?\s+a\s+\$?(\d+(?:\.\d+)?)(?:\s*(?:dólares?|dolares?|pesos?))?(?:\s+la\s+hora)?(?:\s+(?:por\s+)?(.+))?$""",
        RegexOption.IGNORE_CASE
    )

    private val CONTEXTUAL_FLAT_AMOUNT_ES = Regex(
        """^(?:agrega\s+)?(?:servicio|materiales|drywall|pintura|carpinteria|carpintería|piso|techo|electricidad|plomeria|plomería|trabajo|cobro),?\s+\$?(\d+(?:\.\d{1,2})?)\s*(?:dólares?|dolares?|pesos?)?(?:\s+(?:por\s+)?(.+))?$""",
        RegexOption.IGNORE_CASE
    )

    private val CONTEXTUAL_FLAT_AMOUNT_REVERSE_ES = Regex(
        """^(?:agrega\s+)?(?:por\s+)?(.+?)\s+,?\s*\$?(\d+(?:\.\d{1,2})?)\s*(?:dólares?|dolares?|pesos?)?$""",
        RegexOption.IGNORE_CASE
    )

    private fun isSpanish(): Boolean = AppLocale.fromSystem() == AppLocale.Spanish

    fun parse(command: String): ForemanRoute.LocalMacro? {
        val text = command.trim()
        if (text.isBlank()) return null
        val es = isSpanish()

        val activeTab = ForemanRuntimeBinding.current().activeTab
        if (activeTab == AppTab.NewInvoice) {
            val sendPhrases = if (es)
                listOf("enviar esta factura", "enviar factura", "envíala", "enviala")
            else
                listOf("send this invoice", "send invoice", "send it")
            if (sendPhrases.any { text.equals(it, ignoreCase = true) }) {
                return ForemanRoute.LocalMacro(
                    toolName = ToolName.SaveInvoiceFromDraft,
                    arguments = SaveInvoiceFromDraftArgs(isEstimate = false, autoShare = true)
                )
            }

            val receiptPhrases = if (es)
                listOf("adjuntar recibo", "adjuntar último recibo", "escanear recibo", "escanear último recibo")
            else
                listOf("attach receipt", "attach last receipt", "scan last receipt", "scan receipt")
            if (receiptPhrases.any { text.equals(it, ignoreCase = true) }) {
                val clientName = ForemanRuntimeBinding.current().selectedClientName?.let {
                    com.fordham.toolbelt.domain.model.ClientName(it)
                }
                return ForemanRoute.LocalMacro(
                    toolName = ToolName.ScanLastReceipt,
                    arguments = ScanLastReceiptArgs(clientName = clientName)
                )
            }

            val contextualHours = if (es) CONTEXTUAL_HOURS_AT_RATE_ES else CONTEXTUAL_HOURS_AT_RATE_EN
            contextualHours.matchEntire(text)?.let { match ->
                val quantity = match.groupValues[1].toDoubleOrNull() ?: 1.0
                val unitPrice = match.groupValues[2].toDoubleOrNull() ?: 0.0
                val laborLabel = if (es) "Mano de obra" else "Labor"
                val description = match.groupValues.getOrNull(3)?.trim()?.takeIf { it.isNotBlank() } ?: laborLabel
                return ForemanRoute.LocalMacro(
                    toolName = ToolName.AppendDraftLines,
                    arguments = AppendDraftLinesArgs(
                        lineItems = listOf(
                            DraftLineItemInput(
                                description = NaturalLanguage(description),
                                amount = quantity * unitPrice,
                                category = NaturalLanguage(laborLabel),
                                quantity = quantity,
                                unitPrice = unitPrice
                            )
                        )
                    )
                )
            }

            val contextualFlat = if (es) CONTEXTUAL_FLAT_AMOUNT_ES else CONTEXTUAL_FLAT_AMOUNT_EN
            contextualFlat.matchEntire(text)?.let { match ->
                val amount = match.groupValues[1].toDoubleOrNull() ?: 0.0
                val serviceLabel = if (es) "Servicio" else "Service"
                val description = match.groupValues.getOrNull(2)?.trim()?.takeIf { it.isNotBlank() } ?: serviceLabel
                val materialsWord = if (es) "materiales" else "materials"
                val materialsLabel = if (es) "Materiales" else "Materials"
                val category = if (text.contains(materialsWord, ignoreCase = true)) materialsLabel else serviceLabel
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

            val contextualFlatReverse = if (es) CONTEXTUAL_FLAT_AMOUNT_REVERSE_ES else CONTEXTUAL_FLAT_AMOUNT_REVERSE_EN
            contextualFlatReverse.matchEntire(text)?.let { match ->
                val description = match.groupValues[1].trim()
                val amount = match.groupValues[2].toDoubleOrNull() ?: 0.0
                val bannedWords = if (es) listOf("pestaña", "pantalla") else listOf("tab", "screen")
                val serviceLabel = if (es) "Servicio" else "Service"
                if (description.isNotBlank() && bannedWords.none { description.contains(it, ignoreCase = true) }) {
                    return ForemanRoute.LocalMacro(
                        toolName = ToolName.AppendDraftLines,
                        arguments = AppendDraftLinesArgs(
                            lineItems = listOf(
                                DraftLineItemInput(
                                    description = NaturalLanguage(description),
                                    amount = amount,
                                    category = NaturalLanguage(serviceLabel),
                                    quantity = 1.0,
                                    unitPrice = amount
                                )
                            )
                        )
                    )
                }
            }
        }

        // Complex dictation (phone/email/long) bypasses local macros -> Gemini handles it.
        val phoneWord = if (es) "teléfono" else "phone"
        val phoneWordAlt = "telefono"
        val emailWord = if (es) "correo" else "email"
        if (text.contains(phoneWord, ignoreCase = true) ||
            text.contains(phoneWordAlt, ignoreCase = true) ||
            text.contains(emailWord, ignoreCase = true) ||
            text.length > 140
        ) {
            return null
        }

        val unbilled = if (es) UNBILLED_ES else UNBILLED_EN
        unbilled.matchEntire(text)?.let { match ->
            val client = cleanClientName(match.groupValues[1]) ?: return null
            return ForemanRoute.LocalMacro(
                toolName = ToolName.QuickInvoiceFromUnbilledReceipts,
                arguments = QuickInvoiceFromUnbilledReceiptsArgs(clientName = NaturalLanguage(client))
            )
        }

        val duplicateAndEdit = if (es) DUPLICATE_AND_EDIT_ES else DUPLICATE_AND_EDIT_EN
        duplicateAndEdit.matchEntire(text)?.let { match ->
            val client = cleanClientName(match.groupValues[1]) ?: return null
            val lineItem = parseAdditionalLineItem(match.groupValues[2], es) ?: return null
            return ForemanRoute.LocalMacro(
                toolName = ToolName.DuplicateAndEdit,
                arguments = DuplicateAndEditArgs(
                    clientName = NaturalLanguage(client),
                    additionalLineItems = listOf(lineItem)
                )
            )
        }

        val duplicate = if (es) DUPLICATE_ES else DUPLICATE_EN
        duplicate.matchEntire(text)?.let { match ->
            val client = cleanClientName(match.groupValues[1]) ?: return null
            return duplicateMacro(client)
        }

        val sameAsLast = if (es) SAME_AS_LAST_ES else SAME_AS_LAST_EN
        sameAsLast.matchEntire(text)?.let { match ->
            val client = cleanClientName(match.groupValues[1]) ?: return null
            return duplicateMacro(client)
        }

        val newClientInvoice = if (es) NEW_CLIENT_INVOICE_ES else NEW_CLIENT_INVOICE_EN
        newClientInvoice.matchEntire(text)?.let { match ->
            val client = cleanClientName(match.groupValues[1]) ?: return null
            val invoiceBody = match.groupValues[2].trim()
            return parseQuickClientAndInvoice(client, invoiceBody, es)
        }

        return parseQuickInvoice(text, es)
    }

    private fun duplicateMacro(clientName: String): ForemanRoute.LocalMacro =
        ForemanRoute.LocalMacro(
            toolName = ToolName.DuplicateLastInvoice,
            arguments = DuplicateLastInvoiceArgs(clientName = NaturalLanguage(clientName))
        )

    private fun parseQuickClientAndInvoice(clientName: String, invoiceBody: String, es: Boolean): ForemanRoute.LocalMacro? {
        val amountIndicator = if (es) AMOUNT_INDICATOR_ES else AMOUNT_INDICATOR_EN
        if (!amountIndicator.containsMatchIn(invoiceBody)) return null
        val lineItems = parseInvoiceBodyLineItems(invoiceBody, es) ?: return null
        return ForemanRoute.LocalMacro(
            toolName = ToolName.QuickClientAndInvoice,
            arguments = QuickClientAndInvoiceArgs(clientName = NaturalLanguage(clientName), lineItems = lineItems)
        )
    }

    private fun parseInvoiceBodyLineItems(invoiceBody: String, es: Boolean): List<DraftLineItemInput>? {
        val body = invoiceBody.trim()
        val laborLabel = if (es) "Mano de obra" else "Labor"
        val serviceLabel = if (es) "Servicio" else "Service"
        val hoursPattern = if (es)
            Regex("""^(\d+(?:\.\d+)?)\s*(?:horas?|hrs?|h)\s+a\s+\$?(\d+(?:\.\d+)?)(?:\s+(?:por\s+)?(.+))?$""", RegexOption.IGNORE_CASE)
        else
            Regex("""^(\d+(?:\.\d+)?)\s*(?:hours?|hrs?|h)\s+at\s+\$?(\d+(?:\.\d+)?)(?:\s+(?:for\s+)?(.+))?$""", RegexOption.IGNORE_CASE)
        
        hoursPattern.matchEntire(body)?.let { match ->
            val quantity = match.groupValues[1].toDoubleOrNull() ?: return null
            val unitPrice = match.groupValues[2].toDoubleOrNull() ?: return null
            val description = match.groupValues.getOrNull(3)?.trim()?.takeIf { it.isNotBlank() } ?: laborLabel
            return listOf(
                DraftLineItemInput(
                    description = NaturalLanguage(description),
                    amount = quantity * unitPrice,
                    category = NaturalLanguage(laborLabel),
                    quantity = quantity,
                    unitPrice = unitPrice
                )
            )
        }

        val addLineAmount = if (es) ADD_LINE_AMOUNT_ES else ADD_LINE_AMOUNT_EN
        addLineAmount.matchEntire(body)?.let { match ->
            val amount = match.groupValues[1].toDoubleOrNull() ?: return null
            val description = match.groupValues[2].trim().ifBlank { serviceLabel }
            return listOf(
                DraftLineItemInput(
                    description = NaturalLanguage(description),
                    amount = amount,
                    category = NaturalLanguage(serviceLabel),
                    quantity = 1.0,
                    unitPrice = amount
                )
            )
        }
        return null
    }

    private fun parseQuickInvoice(text: String, es: Boolean): ForemanRoute.LocalMacro? {
        val amountIndicator = if (es) AMOUNT_INDICATOR_ES else AMOUNT_INDICATOR_EN
        if (!amountIndicator.containsMatchIn(text)) return null
        val verbPattern = if (es) QUICK_INVOICE_VERB_ES else QUICK_INVOICE_VERB_EN
        val verbMatch = verbPattern.matchEntire(text) ?: return null
        val remainder = verbMatch.groupValues[1].trim()
        if (remainder.isBlank()) return null

        val laborLabel = if (es) "Mano de obra" else "Labor"
        val serviceLabel = if (es) "Servicio" else "Service"
        val hoursAtRate = if (es) HOURS_AT_RATE_ES else HOURS_AT_RATE_EN
        
        hoursAtRate.matchEntire(remainder)?.let { match ->
            val client = cleanClientName(match.groupValues[1]) ?: return null
            val quantity = match.groupValues[2].toDoubleOrNull() ?: return null
            val unitPrice = match.groupValues[3].toDoubleOrNull() ?: return null
            val description = match.groupValues.getOrNull(4)?.trim()?.takeIf { it.isNotBlank() } ?: laborLabel
            return quickInvoiceMacro(
                clientName = client,
                lineItems = listOf(
                    DraftLineItemInput(
                        description = NaturalLanguage(description),
                        amount = quantity * unitPrice,
                        category = NaturalLanguage(laborLabel),
                        quantity = quantity,
                        unitPrice = unitPrice
                    )
                )
            )
        }

        val flatAmount = if (es) FLAT_AMOUNT_ES else FLAT_AMOUNT_EN
        flatAmount.matchEntire(remainder)?.let { match ->
            val client = cleanClientName(match.groupValues[1]) ?: return null
            val amount = match.groupValues[2].toDoubleOrNull() ?: return null
            val description = match.groupValues.getOrNull(3)?.trim()?.takeIf { it.isNotBlank() } ?: serviceLabel
            return quickInvoiceMacro(
                clientName = client,
                lineItems = listOf(
                    DraftLineItemInput(
                        description = NaturalLanguage(description),
                        amount = amount,
                        category = NaturalLanguage(serviceLabel),
                        quantity = 1.0,
                        unitPrice = amount
                    )
                )
            )
        }
        return null
    }

    private fun parseAdditionalLineItem(raw: String, es: Boolean): DraftLineItemInput? {
        val text = raw.trim()
        val serviceLabel = if (es) "Servicio" else "Service"
        val addLineAmount = if (es) ADD_LINE_AMOUNT_ES else ADD_LINE_AMOUNT_EN
        
        addLineAmount.matchEntire(text)?.let { match ->
            val amount = match.groupValues[1].toDoubleOrNull() ?: return null
            val description = match.groupValues[2].trim().ifBlank { serviceLabel }
            return DraftLineItemInput(
                description = NaturalLanguage(description),
                amount = amount,
                category = NaturalLanguage(serviceLabel),
                quantity = 1.0,
                unitPrice = amount
            )
        }
        return parseInvoiceBodyLineItems(text, es)?.firstOrNull()
    }

    private fun quickInvoiceMacro(clientName: String, lineItems: List<DraftLineItemInput>): ForemanRoute.LocalMacro =
        ForemanRoute.LocalMacro(
            toolName = ToolName.QuickInvoice,
            arguments = QuickInvoiceArgs(clientName = NaturalLanguage(clientName), lineItems = lineItems)
        )

    private fun cleanClientName(raw: String): String? {
        val cleaned = raw.trim()
            .replace(Regex("""^[.!?\s]+"""), "")
            .replace(Regex("""[.!?]+$"""), "")
            .replace(Regex("""\s+please$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+por\s+favor$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""^to\s+""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""^for\s+""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""^from\s+""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""^a\s+""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""^para\s+""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""^de\s+""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+for$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+to$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+from$""", RegexOption.IGNORE_CASE), "")
            .trim()
        
        if (cleaned.length < 2) return null
        val lower = cleaned.lowercase()
        val hasConnector = lower.contains(" for ") ||
            lower.contains(" at ") ||
            lower.contains(" doing ") ||
            lower.contains(" repair") ||
            lower.contains(" replace") ||
            lower.contains(" install") ||
            lower.contains(" fix") ||
            lower.contains(" para ") ||
            lower.contains(" por ") ||
            lower.contains(" reparar") ||
            lower.contains(" instalar") ||
            lower.contains(" factura") ||
            lower.contains(" cobrar")
        
        val words = cleaned.split(Regex("""\s+"""))
        if (hasConnector || words.size > 4 || cleaned.length > 30) {
            return null
        }
        return cleaned
    }
}
