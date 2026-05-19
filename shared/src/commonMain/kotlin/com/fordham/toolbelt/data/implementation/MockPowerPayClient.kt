package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.remote.PowerPayClient
import com.fordham.toolbelt.data.remote.PowerPayClientOutcome
import com.fordham.toolbelt.data.remote.PowerPayCreatePaymentRequestDto
import com.fordham.toolbelt.data.remote.PowerPayPaymentResponseDto
import com.fordham.toolbelt.util.randomUUID
import kotlinx.datetime.Clock

class MockPowerPayClient : PowerPayClient {
    private val payments = mutableListOf<PowerPayPaymentResponseDto>()

    override suspend fun createInvoicePayment(request: PowerPayCreatePaymentRequestDto): PowerPayClientOutcome<PowerPayPaymentResponseDto> {
        val response = PowerPayPaymentResponseDto(
            paymentId = randomUUID(),
            invoiceId = request.invoiceId,
            clientName = request.clientName,
            amountUsd = request.amountUsd,
            requestType = request.requestType,
            provider = request.provider,
            status = "requested",
            paymentLinkUrl = "https://pay.invoicehammer.dev/mock/${request.provider}/${request.invoiceId}",
            assetCode = if (request.provider == "stellar_usdc") "USDC" else "USD",
            createdAtMillis = Clock.System.now().toEpochMilliseconds()
        )
        payments.removeAll { it.invoiceId == response.invoiceId && it.requestType == response.requestType && it.provider == response.provider }
        payments.add(0, response)
        return PowerPayClientOutcome.Success(response)
    }

    override suspend fun getTransactionHistory(): PowerPayClientOutcome<List<PowerPayPaymentResponseDto>> {
        return PowerPayClientOutcome.Success(payments.toList())
    }
}
