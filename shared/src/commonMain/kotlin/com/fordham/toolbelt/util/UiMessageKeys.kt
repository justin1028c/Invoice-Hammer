package com.fordham.toolbelt.util

/** Stable message keys emitted by ViewModels/platform code; localized at the UI boundary. */
object UiMessageKeys {
    const val PREFIX = "ui::"

    const val CLIENT_NAME_REQUIRED = "${PREFIX}client_name_required"
    const val CLIENT_ADDRESS_REQUIRED = "${PREFIX}client_address_required"
    const val LINE_ITEMS_REQUIRED = "${PREFIX}line_items_required"
    const val WHICH_CLIENT_MEANT = "${PREFIX}which_client_meant"
    const val UNHANDLED_TOOL_REQUEST = "${PREFIX}unhandled_tool_request"
    const val BUSINESS_LOGO_SAVED = "${PREFIX}business_logo_saved"
    const val BUSINESS_LOGO_REMOVED = "${PREFIX}business_logo_removed"
    const val PURCHASE_CANCELLED = "${PREFIX}purchase_cancelled"
    const val LOW_BALANCE = "${PREFIX}low_balance"
    const val PURCHASES_RESTORED = "${PREFIX}purchases_restored"
    const val NO_ACTIVE_SUBSCRIPTION = "${PREFIX}no_active_subscription"
    const val STRIPE_SIGN_IN_REQUIRED = "${PREFIX}stripe_sign_in_required"
    const val STRIPE_BACKEND_URL_REQUIRED = "${PREFIX}stripe_backend_url_required"
    const val PREMIUM_REQUIRED_RECEIPT = "${PREFIX}premium_required_receipt"
    const val PAYMENT_CANCELLED = "${PREFIX}payment_cancelled"
    const val STRIPE_CONNECT_NOT_CONFIGURED = "${PREFIX}stripe_connect_not_configured"
    const val BLUETOOTH_READERS_NOT_ENABLED = "${PREFIX}bluetooth_readers_not_enabled"
    const val PAYMENT_SUCCESS = "${PREFIX}payment_success"
    const val CHECKOUT_LINK_ALREADY_PAID = "${PREFIX}checkout_link_already_paid"
    const val CHECKOUT_LINK_EXPIRED = "${PREFIX}checkout_link_expired"
    const val CAMERA_UNAVAILABLE = "${PREFIX}camera_unavailable"
    const val CAMERA_PERMISSION_REQUIRED = "${PREFIX}camera_permission_required"

    fun aiError(detail: String) = "${PREFIX}ai_error|$detail"
    fun proUnlockedVia(source: String) = "${PREFIX}pro_unlocked|$source"
    fun purchased(displayName: String) = "${PREFIX}purchased|$displayName"
    fun savedTo(path: String) = "${PREFIX}saved_to|$path"
    fun reviewInvoice(clientName: String) = "${PREFIX}review_invoice|$clientName"
    fun couldNotLoadPhoto(detail: String) = "${PREFIX}could_not_load_photo|$detail"
    fun failedProcessReceipt(detail: String) = "${PREFIX}failed_process_receipt|$detail"
    fun paidSecureCheckout(amount: String) = "${PREFIX}paid_secure_checkout|$amount"
    fun paidCardTerminal(amount: String) = "${PREFIX}paid_card_terminal|$amount"
}
