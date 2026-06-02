package com.fordham.toolbelt.domain.model.agent

import com.fordham.toolbelt.domain.model.ClientId

/** Side effects the UI shell must apply after a safe tool run or approved action. */
sealed interface AgentUiEffect {
    data class NavigateToTab(val tab: AppTab) : AgentUiEffect
    data class SelectClient(val clientId: ClientId) : AgentUiEffect
    data class SearchHistory(val query: String) : AgentUiEffect
    data class ShareInvoiceDocument(
        val pdfPath: String,
        val title: String,
        val recipientEmail: String = "",
        val recipientPhone: String = "",
        val subject: String = "",
        val body: String = ""
    ) : AgentUiEffect
    data class ViewPdf(val pdfPath: String) : AgentUiEffect
    data class OpenSupplierStore(
        val supplierId: String,
        val name: String,
        val packageName: String,
        val webUrl: String
    ) : AgentUiEffect
}
