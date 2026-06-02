package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.remote.PowerPayWebhookEventDto
import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.util.HmacSha256
import kotlinx.serialization.json.Json

/**
 * Verifies PowerPay webhook signatures (HMAC-SHA256 over `timestamp.body`).
 * Use on your Vercel/Node backend; same algorithm as the official SDK `pay.webhooks.verify(req)`.
 */
object PowerPayWebhookVerifier {
    private const val MAX_AGE_SECONDS = 300
    private val json = Json { ignoreUnknownKeys = true }

    sealed interface VerifyOutcome {
        data class Valid(val event: PowerPayWebhookEventDto) : VerifyOutcome
        data class Invalid(val reason: FailureMessage) : VerifyOutcome
    }

    fun verify(
        signingSecret: String,
        timestampHeader: String?,
        signatureHeader: String?,
        eventIdHeader: String?,
        rawBody: String,
        nowEpochSeconds: Long
    ): VerifyOutcome {
        if (signingSecret.isBlank()) {
            return VerifyOutcome.Invalid(FailureMessage("PowerPay signing secret is not configured."))
        }
        if (timestampHeader.isNullOrBlank() || signatureHeader.isNullOrBlank()) {
            return VerifyOutcome.Invalid(FailureMessage("Missing PowerPay webhook signature headers."))
        }

        val timestamp = timestampHeader.toLongOrNull()
            ?: return VerifyOutcome.Invalid(FailureMessage("Invalid x-powerpay-timestamp header."))

        val age = nowEpochSeconds - timestamp
        if (age > MAX_AGE_SECONDS) {
            return VerifyOutcome.Invalid(FailureMessage("Webhook timestamp is too old (replay protection)."))
        }

        val receivedHex = signatureHeader.removePrefix("v1=").trim().lowercase()
        val expectedHex = computeSignatureHex(signingSecret, timestampHeader, rawBody)

        if (!timingSafeEquals(expectedHex, receivedHex)) {
            return VerifyOutcome.Invalid(FailureMessage("Webhook signature mismatch."))
        }

        return try {
            val event = json.decodeFromString<PowerPayWebhookEventDto>(rawBody)
            if (eventIdHeader != null && event.eventId != null && event.eventId != eventIdHeader) {
                return VerifyOutcome.Invalid(FailureMessage("Webhook event id header does not match payload."))
            }
            VerifyOutcome.Valid(event)
        } catch (e: Exception) {
            VerifyOutcome.Invalid(FailureMessage(e.message ?: "Invalid webhook JSON payload."))
        }
    }

    fun computeSignatureHex(signingSecret: String, timestamp: String, rawBody: String): String {
        val payload = "$timestamp.$rawBody"
        return HmacSha256.hexDigest(
            secret = signingSecret.encodeToByteArray(),
            message = payload.encodeToByteArray()
        )
    }

    private fun timingSafeEquals(expected: String, received: String): Boolean {
        if (expected.length != received.length) return false
        var result = 0
        for (i in expected.indices) {
            result = result or (expected[i].code xor received[i].code)
        }
        return result == 0
    }
}
