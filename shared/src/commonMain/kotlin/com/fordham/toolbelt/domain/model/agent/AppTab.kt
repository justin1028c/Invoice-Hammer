package com.fordham.toolbelt.domain.model.agent

/** Main shell tabs — index and [navLabel] match [MainBottomBar] left-to-right. */
enum class AppTab(val pageIndex: Int, val navLabel: String) {
    NewInvoice(0, "NEW"),
    History(1, "PAST"),
    Receipts(2, "RECEIPTS"),
    Stats(3, "STATS"),
    Suppliers(4, "STORES"),
    Clients(5, "CLIENTS"),
    Settings(6, "SETTINGS");

    companion object {
        val NAV_LABELS: String = entries.joinToString("|") { it.navLabel }

        fun fromName(raw: String): AppTab? = when (raw.uppercase().replace(" ", "_").trim()) {
            "NEW", "NEW_INVOICE", "INVOICE", "NEW_TAB" -> NewInvoice
            "HISTORY", "PAST", "PAST_INVOICES", "PAST_TAB" -> History
            "RECEIPTS", "RECEIPT" -> Receipts
            "STATS", "STATISTICS", "BENTO" -> Stats
            "SUPPLIERS", "SUPPLIER", "STORES", "STORE" -> Suppliers
            "CLIENTS", "CLIENT" -> Clients
            "SETTINGS", "SETTING" -> Settings
            else -> entries.firstOrNull {
                it.name.equals(raw, ignoreCase = true) || it.navLabel.equals(raw, ignoreCase = true)
            }
        }
    }
}
