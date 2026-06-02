package com.fordham.toolbelt.domain.model.agent

/**
 * Bottom-bar tab navigation — labels match [MainBottomBar] left-to-right order.
 * Used for local routing without a Gemini round-trip.
 */
object ForemanTabNavigation {
    /** Same order as the main navigation bar. */
    val BOTTOM_BAR_ORDER: List<AppTab> = listOf(
        AppTab.NewInvoice,
        AppTab.History,
        AppTab.Receipts,
        AppTab.Stats,
        AppTab.Suppliers,
        AppTab.Clients,
        AppTab.Settings
    )

    private val NAV_VERBS = Regex(
        """^(?:open|go\s+to|switch\s+to|show|take\s+me\s+to|navigate\s+to)\s+(?:the\s+)?(.+?)(?:\s+tab)?[.!?\s]*$""",
        RegexOption.IGNORE_CASE
    )

    private val TAB_ONLY = Regex(
        """^(new|past|receipts|stats|stores|clients|settings|new\s+invoice|invoice)(?:\s+tab)?[.!?\s]*$""",
        RegexOption.IGNORE_CASE
    )

    /** Nav + real work in one utterance → Gemini handles it. */
    private val COMPOUND_WORK = Regex(
        """\band\b\s+(?:bill|charge|find|search|create|save|send|text|email|invoice\s+\w|draft)""",
        RegexOption.IGNORE_CASE
    )

    /** Starts as invoice/client work, not tab switch. */
    private val WORK_FIRST = Regex(
        """^(?:find|search|bill|charge|create|save|send|text|email|invoice\s+(?!tab\b)[\w\s]+)""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Resolves a navigation-only utterance to a tab, or null if Gemini should handle it.
     */
    fun parseNavigationOnly(command: String): AppTab? {
        val trimmed = ForemanSttNormalizer.normalize(command)
        if (trimmed.isBlank()) return null
        if (isCompoundWorkCommand(trimmed)) return null

        TAB_ONLY.matchEntire(trimmed)?.let { match ->
            return tabFromPhrase(match.groupValues[1])
        }

        NAV_VERBS.matchEntire(trimmed)?.let { match ->
            val target = match.groupValues[1].trim()
            if (target.isNotBlank()) {
                tabFromPhrase(target)?.let { return it }
            }
        }

        if (trimmed.split(" ").size <= 2) {
            return tabFromPhrase(trimmed)
        }
        return null
    }

    fun isNavigationOnly(command: String): Boolean = parseNavigationOnly(command) != null

    private fun isCompoundWorkCommand(text: String): Boolean {
        if (COMPOUND_WORK.containsMatchIn(text)) return true
        if (WORK_FIRST.containsMatchIn(text.trim())) return true
        return false
    }

    private fun tabFromPhrase(phrase: String): AppTab? {
        val tokens = phrase.lowercase()
            .replace("invoice hammer", "")
            .trim()
        tabFromToken(tokens)?.let { return it }
        return when {
            tokens.contains("new invoice") ||
                tokens == "invoice" ||
                tokens.contains("invoice tab") ||
                tokens.contains("new tab") ||
                tokens.contains("editor") ||
                tokens.contains("draft board") ||
                tokens.contains("draft") ||
                tokens == "new" -> AppTab.NewInvoice
            tokens.contains("past") ||
                tokens.contains("history") ||
                tokens.contains("previous") ||
                tokens.contains("jobs") ||
                tokens.contains("records") -> AppTab.History
            tokens.contains("receipt") ||
                tokens.contains("expense") ||
                tokens.contains("camera") -> AppTab.Receipts
            tokens.contains("stat") ||
                tokens.contains("bento") ||
                tokens.contains("dashboard") ||
                tokens.contains("finance") ||
                tokens.contains("revenue") ||
                tokens.contains("analytics") -> AppTab.Stats
            tokens.contains("store") ||
                tokens.contains("supplier") ||
                tokens.contains("material") ||
                tokens.contains("shop") -> AppTab.Suppliers
            tokens.contains("client") ||
                tokens.contains("customer") ||
                tokens.contains("contact") -> AppTab.Clients
            tokens.contains("setting") ||
                tokens.contains("option") ||
                tokens.contains("profile") -> AppTab.Settings
            else -> AppTab.fromName(tokens)
        }
    }

    private fun tabFromToken(token: String): AppTab? = when (token.lowercase()) {
        "new", "new invoice", "invoice" -> AppTab.NewInvoice
        "past" -> AppTab.History
        "receipts", "receipt" -> AppTab.Receipts
        "stats", "stat" -> AppTab.Stats
        "stores", "store" -> AppTab.Suppliers
        "clients", "client" -> AppTab.Clients
        "settings", "setting" -> AppTab.Settings
        else -> AppTab.fromName(token)
    }
}
