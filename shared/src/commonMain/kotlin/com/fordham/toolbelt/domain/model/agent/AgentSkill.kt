package com.fordham.toolbelt.domain.model.agent

/**
 * Isolated competencies for Foreman LLM agent, allowing dynamic prompt reduction.
 */
sealed interface AgentSkill {
    val systemInstruction: String
    val allowedTools: List<ToolName>

    data object FinanceSkills : AgentSkill {
        override val systemInstruction: String = """
            You are the financial clerk skill of Foreman AI.
            You excel at billing, invoice creation, modifying draft items, and estimating costs.
            Only answer or trigger actions related to payments, tax rates, line items, deposits, and drafts.
        """.trimIndent()

        override val allowedTools: List<ToolName> = listOf(
            ToolName.CreateDraftInvoice,
            ToolName.UpdateDraftInvoice,
            ToolName.SaveInvoiceFromDraft,
            ToolName.QuickInvoice,
            ToolName.QuickClientAndInvoice,
            ToolName.AppendDraftLines,
            ToolName.DuplicateLastInvoice,
            ToolName.DuplicateAndEdit,
            ToolName.QuickInvoiceFromUnbilledReceipts,
            ToolName.QuickSendInvoice,
            ToolName.SendInvoiceEmail,
            ToolName.SendInvoiceSms,
            ToolName.DeleteInvoiceForApproval,
            ToolName.GetProfitGuardianStatus,
            ToolName.DetectChangeOrders,
            ToolName.GetDailyBriefing,
            ToolName.CreateChangeOrder
        )
    }

    data object CrmSkills : AgentSkill {
        override val systemInstruction: String = """
            You are the customer relationship manager skill of Foreman AI.
            You excel at searching clients, creating clients, looking up balances, and details.
            Only answer or trigger actions related to client records, contact details, or job notes.
        """.trimIndent()

        override val allowedTools: List<ToolName> = listOf(
            ToolName.SearchClients,
            ToolName.SelectClient,
            ToolName.GetClientDetails,
            ToolName.GetUnbilledReceipts,
            ToolName.CreateClient,
            ToolName.QuickClientLookup,
            ToolName.AddJobNote,
            ToolName.GetProfitGuardianStatus,
            ToolName.DetectChangeOrders
        )
    }

    data object NavigationSkills : AgentSkill {
        override val systemInstruction: String = """
            You are the app navigation clerk skill of Foreman AI.
            You excel at taking the user to specific screens, history tables, or opening documents.
            Only answer or trigger actions related to switching tabs, viewing PDFs, or opening supplier storefronts.
        """.trimIndent()

        override val allowedTools: List<ToolName> = listOf(
            ToolName.OpenTab,
            ToolName.OpenLastInvoice,
            ToolName.OpenSupplier
        )
    }

    data object HelpSkills : AgentSkill {
        override val systemInstruction: String = """
            You are the customer assistant and helper for Foreman AI.
            You excel at explaining how the app works, outlining its features, and guiding the user.
            Explain clearly that the app has three main functional areas:
            1. Billing & Finance: Invoice creation, estimate drafting, managing line items/costs, and sending PDFs via email/SMS.
            2. Customer Relationship Management (CRM): Creating/searching clients, managing contact details, and writing job notes.
            3. Navigation & Actions: Switching tabs, viewing documents, scanning receipt photos, and opening supplier stores.
            Always be thorough, complete, and professional in your explanations.
        """.trimIndent()

        override val allowedTools: List<ToolName> = listOf(
            ToolName.OpenTab,
            ToolName.QuickClientLookup,
            ToolName.SearchInvoiceHistory
        )
    }
}

object AgentSkillClassifier {
    fun classify(userInput: String): AgentSkill {
        val query = userInput.trim().lowercase()

        // Help & capability requests and common greetings
        val hasHelpIntent = query.containsAny(
            "what does the app do", "what can you do", "help", "features", "capabilities",
            "how to use", "how do i", "guide", "instructions", "explain", "comprehensive list",
            "what is this app", "hello", "hi", "hey", "hola", "buenos dias", "good morning", "good afternoon"
        )
        if (hasHelpIntent) return AgentSkill.HelpSkills

        // Finance indicators take priority — if invoice/bill/charge keywords are present,
        // treat as Finance even if an address or contact keyword also appears.
        val hasFinanceIntent = query.containsAny(
            "invoice", "bill ", "charge", "estimate", "receipt", "draft",
            "line item", "tax", "deposit", "send invoice", "same as last",
            "labor", "append", "add labor", "add service", "add materials", "send it", "send this",
            "profit", "guardian", "variance", "briefing", "change order", "budget", "overrun", "cost warning"
        )
        if (hasFinanceIntent) return AgentSkill.FinanceSkills

        // CRM — only when no finance intent is present
        val hasCrmIntent = query.containsAny(
            "new client", "add client", "find client", "search client",
            "client", "customer", "find ", "search ", "contact", "phone", "email",
            "note", "address", "job site", "billing address", "invoice address", "client address"
        )
        if (hasCrmIntent) return AgentSkill.CrmSkills

        // Navigation
        val hasNavIntent = query.containsAny(
            "open", "go to", "tab", "screen", "view", "show me", "supplier", "store"
        )
        if (hasNavIntent) return AgentSkill.NavigationSkills

        return AgentSkill.FinanceSkills
    }

    private fun String.containsAny(vararg tokens: String) = tokens.any { this.contains(it) }
}
