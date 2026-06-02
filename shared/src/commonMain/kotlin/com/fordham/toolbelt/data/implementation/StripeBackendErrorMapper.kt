package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.remote.StripeBackendErrorBody
import com.fordham.toolbelt.domain.model.FailureMessage
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json

private val stripeErrorJson = Json { ignoreUnknownKeys = true }

internal suspend fun mapStripeBackendFailure(
    response: HttpResponse,
    fallbackLabel: String
): StripePaymentIntentFailureDetails {
    val body = runCatching { response.bodyAsText() }.getOrDefault("")
    val parsed = runCatching {
        stripeErrorJson.decodeFromString<StripeBackendErrorBody>(body)
    }.getOrNull()
    val detail = parsed?.error?.trim().orEmpty()
    val onboardingUrl = parsed?.onboardingUrl?.trim()?.takeIf { it.isNotBlank() }
    val message = when {
        onboardingUrl != null ->
            detail.ifBlank { "Finish Stripe Connect setup before accepting payments." } +
                " Use Settings → STRIPE PAYOUTS."
        detail.isNotBlank() -> detail
        else -> "$fallbackLabel (${response.status.value})."
    }
    return StripePaymentIntentFailureDetails(
        error = FailureMessage(message),
        actionUrl = onboardingUrl
    )
}

internal data class StripePaymentIntentFailureDetails(
    val error: FailureMessage,
    val actionUrl: String? = null
)
