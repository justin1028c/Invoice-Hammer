package com.fordham.toolbelt.ui.viewmodel

import com.fordham.toolbelt.domain.model.*

data class NewInvoiceUiState(
    val clientName: String = "",
    val clientAddress: String = "",
    val taxText: String = "7.0",
    val depositCollected: String = "",
    val hourlyRate: String = "50.0",
    val logoUri: String? = null,
    val lineItems: List<LineItem> = emptyList(),
    val selectedCategory: String = "Drywall",
    val itemDesc: String = "",
    val itemAmt: String = "",
    val isProcessingAi: Boolean = false,
    val pendingAi: List<LineItem> = emptyList(),
    val showAiConf: Boolean = false,
    val showClientDropdown: Boolean = false,
    val showCategoryDropdown: Boolean = false,
    val isListening: Boolean = false,
    val timerRunning: Boolean = false,
    val elapsedSeconds: Long = 0L,
    val startTime: Long = 0L,
    val saveToClientDirectory: Boolean = false,
    val canAddManual: Boolean = false,
    val canSave: Boolean = false,
    val errorMessage: String? = null,
    val capturedPhotos: List<CapturedJobPhoto> = emptyList(),
    val businessLogoSaved: Boolean = false,
    val availableReceipts: List<ReceiptItem> = emptyList(),
    val showReceiptPicker: Boolean = false,
    val laborHours: Double? = null,
    val laborRate: Double? = null,
    val depositAmount: Double = 0.0,
    val taxRatePercent: Double = 7.0,
    val discountPercent: Double = 0.0,
    val notes: String = "",
    val confidenceScore: Double = 1.0,
    val userSummary: String = "",
    val validationIssues: List<String> = emptyList()
) {
    val formattedTime: String
        get() {
            val hours = elapsedSeconds / 3600
            val minutes = (elapsedSeconds % 3600) / 60
            val seconds = elapsedSeconds % 60
            return "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
        }
}

sealed interface NewInvoiceIntent {
    data class OnClientNameChange(val name: String) : NewInvoiceIntent
    data class OnClientAddressChange(val address: String) : NewInvoiceIntent
    data class OnTaxTextChange(val tax: String) : NewInvoiceIntent
    data class OnDepositCollectedChange(val amt: String) : NewInvoiceIntent
    data class OnHourlyRateChange(val rate: String) : NewInvoiceIntent
    data class OnLogoUriChange(val uri: String?) : NewInvoiceIntent
    data class OnCategoryChange(val cat: String) : NewInvoiceIntent
    data class OnSaveToClientDirectoryChange(val save: Boolean) : NewInvoiceIntent
    data class OnItemDescChange(val desc: String) : NewInvoiceIntent
    data class OnItemAmtChange(val amt: String) : NewInvoiceIntent
    data class OnPhotoCaptured(val uri: String, val phase: JobPhotoPhase) : NewInvoiceIntent
    data class RemovePhoto(val uri: String) : NewInvoiceIntent
    data class SetClientDropdownVisible(val visible: Boolean) : NewInvoiceIntent
    data class SetCategoryDropdownVisible(val visible: Boolean) : NewInvoiceIntent
    data class OnListeningStateChange(val visible: Boolean) : NewInvoiceIntent
    data class OnShowAiConfChange(val visible: Boolean) : NewInvoiceIntent
    data class SetReceiptPickerVisible(val visible: Boolean) : NewInvoiceIntent
    data object ClearError : NewInvoiceIntent
    data object ToggleTimer : NewInvoiceIntent
    data object AddManualLineItem : NewInvoiceIntent
    data object BillLabor : NewInvoiceIntent
    data class RemoveLineItem(val item: LineItem) : NewInvoiceIntent
    data object AcceptAiItems : NewInvoiceIntent
    data class ProcessInvoiceAi(val categories: List<String>) : NewInvoiceIntent
    data class LinkReceipt(val receipt: ReceiptItem, val markupPercent: Double) : NewInvoiceIntent
    data class SaveInvoice(
        val isEstimate: Boolean,
        val settings: BusinessSettings,
        val onGenerated: (String) -> Unit
    ) : NewInvoiceIntent
}

internal data class NewInvoiceTransientState(
    val isProcessingAi: Boolean = false,
    val isListening: Boolean = false,
    val errorMessage: String? = null,
    val showAiConf: Boolean = false,
    val pendingAi: List<LineItem> = emptyList(),
    val showClientDropdown: Boolean = false,
    val showCategoryDropdown: Boolean = false,
    val showReceiptPicker: Boolean = false,
    val availableReceipts: List<ReceiptItem> = emptyList(),
    val clientName: String? = null,
    val clientAddress: String? = null,
    val taxText: String? = null,
    val depositCollected: String? = null,
    val hourlyRate: String? = null,
    val itemDesc: String? = null,
    val itemAmt: String? = null,
    val laborHours: Double? = null,
    val laborRate: Double? = null,
    val depositAmount: Double? = null,
    val taxRatePercent: Double? = null,
    val discountPercent: Double? = null,
    val notes: String? = null,
    val confidenceScore: Double? = null,
    val userSummary: String? = null,
    val validationIssues: List<String>? = null
)
