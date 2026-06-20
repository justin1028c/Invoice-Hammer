package com.fordham.toolbelt.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.fordham.toolbelt.util.UiMessageKeys
import com.fordham.toolbelt.util.ForemanMessageLocalizer
import invoicehammer.composeapp.generated.resources.Res
import invoicehammer.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun rememberLocalizeUiMessage(): (String) -> String {
    val clientNameRequired = stringResource(Res.string.err_client_name_required)
    val clientAddressRequired = stringResource(Res.string.err_client_address_required)
    val lineItemsRequired = stringResource(Res.string.err_line_items_required)
    val whichClientMeant = stringResource(Res.string.which_client)
    val unhandledToolRequest = stringResource(Res.string.foreman_unhandled_tool)
    val businessLogoSaved = stringResource(Res.string.toast_business_logo_saved)
    val businessLogoRemoved = stringResource(Res.string.toast_business_logo_removed)
    val purchaseCancelled = stringResource(Res.string.toast_purchase_cancelled)
    val lowBalance = stringResource(Res.string.toast_low_balance)
    val purchasesRestored = stringResource(Res.string.toast_purchases_restored)
    val noActiveSubscription = stringResource(Res.string.toast_no_active_subscription)
    val stripeSignInRequired = stringResource(Res.string.toast_stripe_sign_in_required)
    val stripeBackendUrlRequired = stringResource(Res.string.toast_stripe_backend_url_required)
    val premiumRequiredReceipt = stringResource(Res.string.err_premium_required_receipt)
    val paymentCancelled = stringResource(Res.string.payment_cancelled)
    val stripeConnectNotConfigured = stringResource(Res.string.stripe_connect_not_configured)
    val bluetoothReadersNotEnabled = stringResource(Res.string.bluetooth_readers_not_enabled)
    val paymentSuccess = stringResource(Res.string.payment_success)
    val checkoutLinkAlreadyPaid = stringResource(Res.string.checkout_link_already_paid)
    val checkoutLinkExpired = stringResource(Res.string.checkout_link_expired)
    val cameraUnavailable = stringResource(Res.string.toast_camera_unavailable)
    val cameraPermissionRequired = stringResource(Res.string.toast_camera_permission_required)

    return remember(
        clientNameRequired,
        clientAddressRequired,
        lineItemsRequired,
        whichClientMeant,
        unhandledToolRequest,
        businessLogoSaved,
        businessLogoRemoved,
        purchaseCancelled,
        lowBalance,
        purchasesRestored,
        noActiveSubscription,
        stripeSignInRequired,
        stripeBackendUrlRequired,
        premiumRequiredReceipt,
        paymentCancelled,
        stripeConnectNotConfigured,
        bluetoothReadersNotEnabled,
        paymentSuccess,
        checkoutLinkAlreadyPaid,
        checkoutLinkExpired,
        cameraUnavailable,
        cameraPermissionRequired
    ) {
        { raw ->
            if (raw.startsWith(UiMessageKeys.PREFIX)) {
                val body = raw.removePrefix(UiMessageKeys.PREFIX)
                val pipe = body.indexOf('|')
                val key = if (pipe >= 0) body.substring(0, pipe) else body
                val arg = if (pipe >= 0) body.substring(pipe + 1) else ""
                when (key) {
                    "client_name_required" -> clientNameRequired
                    "client_address_required" -> clientAddressRequired
                    "line_items_required" -> lineItemsRequired
                    "which_client_meant" -> whichClientMeant
                    "unhandled_tool_request" -> unhandledToolRequest
                    "business_logo_saved" -> businessLogoSaved
                    "business_logo_removed" -> businessLogoRemoved
                    "purchase_cancelled" -> purchaseCancelled
                    "low_balance" -> lowBalance
                    "purchases_restored" -> purchasesRestored
                    "no_active_subscription" -> noActiveSubscription
                    "stripe_sign_in_required" -> stripeSignInRequired
                    "stripe_backend_url_required" -> stripeBackendUrlRequired
                    "premium_required_receipt" -> premiumRequiredReceipt
                    "payment_cancelled" -> paymentCancelled
                    "stripe_connect_not_configured" -> stripeConnectNotConfigured
                    "bluetooth_readers_not_enabled" -> bluetoothReadersNotEnabled
                    "payment_success" -> paymentSuccess
                    "checkout_link_already_paid" -> checkoutLinkAlreadyPaid
                    "checkout_link_expired" -> checkoutLinkExpired
                    "camera_unavailable" -> cameraUnavailable
                    "camera_permission_required" -> cameraPermissionRequired
                    else -> ForemanMessageLocalizer.localize(raw)
                }
            } else {
                when (raw) {
                    "Payment checkout was cancelled." -> paymentCancelled
                    "Stripe Connect backend is not configured." -> stripeConnectNotConfigured
                    "Bluetooth card readers are not enabled in this build. Use Tap to Pay or Card Terminal." -> bluetoothReadersNotEnabled
                    "Secure payment succeeded! Invoice is marked paid." -> paymentSuccess
                    else -> ForemanMessageLocalizer.localize(raw)
                }
            }
        }
    }
}

@Composable
fun localizeUiMessage(raw: String): String {
    if (raw.startsWith(UiMessageKeys.PREFIX)) {
        val body = raw.removePrefix(UiMessageKeys.PREFIX)
        val pipe = body.indexOf('|')
        val key = if (pipe >= 0) body.substring(0, pipe) else body
        val arg = if (pipe >= 0) body.substring(pipe + 1) else ""
        return when (key) {
            "client_name_required" -> stringResource(Res.string.err_client_name_required)
            "client_address_required" -> stringResource(Res.string.err_client_address_required)
            "line_items_required" -> stringResource(Res.string.err_line_items_required)
            "which_client_meant" -> stringResource(Res.string.which_client)
            "unhandled_tool_request" -> stringResource(Res.string.foreman_unhandled_tool)
            "business_logo_saved" -> stringResource(Res.string.toast_business_logo_saved)
            "business_logo_removed" -> stringResource(Res.string.toast_business_logo_removed)
            "purchase_cancelled" -> stringResource(Res.string.toast_purchase_cancelled)
            "low_balance" -> stringResource(Res.string.toast_low_balance)
            "purchases_restored" -> stringResource(Res.string.toast_purchases_restored)
            "no_active_subscription" -> stringResource(Res.string.toast_no_active_subscription)
            "stripe_sign_in_required" -> stringResource(Res.string.toast_stripe_sign_in_required)
            "stripe_backend_url_required" -> stringResource(Res.string.toast_stripe_backend_url_required)
            "premium_required_receipt" -> stringResource(Res.string.err_premium_required_receipt)
            "payment_cancelled" -> stringResource(Res.string.payment_cancelled)
            "stripe_connect_not_configured" -> stringResource(Res.string.stripe_connect_not_configured)
            "bluetooth_readers_not_enabled" -> stringResource(Res.string.bluetooth_readers_not_enabled)
            "payment_success" -> stringResource(Res.string.payment_success)
            "checkout_link_already_paid" -> stringResource(Res.string.checkout_link_already_paid)
            "checkout_link_expired" -> stringResource(Res.string.checkout_link_expired)
            "camera_unavailable" -> stringResource(Res.string.toast_camera_unavailable)
            "camera_permission_required" -> stringResource(Res.string.toast_camera_permission_required)
            "ai_error" -> stringResource(Res.string.err_ai_error_fmt, arg)
            "pro_unlocked" -> stringResource(Res.string.toast_pro_unlocked_fmt, arg)
            "purchased" -> stringResource(Res.string.toast_purchased_fmt, arg)
            "saved_to" -> stringResource(Res.string.toast_saved_to_fmt, arg)
            "review_invoice" -> stringResource(Res.string.foreman_review_invoice_fmt, arg)
            "could_not_load_photo" -> stringResource(Res.string.err_could_not_load_photo_fmt, arg)
            "failed_process_receipt" -> stringResource(Res.string.err_failed_process_receipt_fmt, arg)
            "paid_secure_checkout" -> stringResource(Res.string.secure_checkout_paid_success, arg)
            "paid_card_terminal" -> stringResource(Res.string.card_terminal_paid_success, arg)
            else -> ForemanMessageLocalizer.localize(raw)
        }
    }

    return when (raw) {
        "Payment checkout was cancelled." -> stringResource(Res.string.payment_cancelled)
        "Stripe Connect backend is not configured." -> stringResource(Res.string.stripe_connect_not_configured)
        "Bluetooth card readers are not enabled in this build. Use Tap to Pay or Card Terminal." ->
            stringResource(Res.string.bluetooth_readers_not_enabled)
        "Secure payment succeeded! Invoice is marked paid." -> stringResource(Res.string.payment_success)
        else -> ForemanMessageLocalizer.localize(raw)
    }
}
