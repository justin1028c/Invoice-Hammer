package com.fordham.toolbelt.domain.deeplink

import io.ktor.http.decodeURLQueryComponent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed interface DeepLinkEvent {
    data class PaymentSuccess(
        val invoiceId: String,
        val sessionId: String? = null,
        val contractorUserId: String? = null
    ) : DeepLinkEvent

    data class PaymentCancelled(
        val invoiceId: String,
        val sessionId: String? = null,
        val contractorUserId: String? = null
    ) : DeepLinkEvent
}

interface DeepLinkDispatcher {
    val events: SharedFlow<DeepLinkEvent>
    fun dispatch(url: String): Boolean
}

class DeepLinkDispatcherImpl : DeepLinkDispatcher {
    private val _events = MutableSharedFlow<DeepLinkEvent>(
        replay = 1,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    override val events = _events.asSharedFlow()

    override fun dispatch(url: String): Boolean {
        if (!url.startsWith("invoicehammer://")) return false
        val uri = url.substring("invoicehammer://".length)
        val parts = uri.split("?", limit = 2)
        val path = parts[0].trimEnd('/')
        val queryParams = if (parts.size > 1) {
            parts[1].split("&").associate { segment ->
                val pair = segment.split("=", limit = 2)
                val key = decodeParam(pair[0])
                val value = if (pair.size > 1) decodeParam(pair[1]) else ""
                key to value
            }
        } else emptyMap()

        val invoiceId = queryParams["invoice_id"] ?: ""
        val sessionId = queryParams["session_id"]?.takeIf { it.isNotBlank() }
        val contractorUserId = queryParams["contractor_user_id"]?.takeIf { it.isNotBlank() }
        return when (path) {
            "payment-success" -> {
                _events.tryEmit(
                    DeepLinkEvent.PaymentSuccess(
                        invoiceId = invoiceId,
                        sessionId = sessionId,
                        contractorUserId = contractorUserId
                    )
                )
                true
            }
            "payment-cancelled" -> {
                _events.tryEmit(
                    DeepLinkEvent.PaymentCancelled(
                        invoiceId = invoiceId,
                        sessionId = sessionId,
                        contractorUserId = contractorUserId
                    )
                )
                true
            }
            else -> false
        }
    }

    private fun decodeParam(value: String): String =
        runCatching { value.decodeURLQueryComponent() }.getOrDefault(value)
}
