package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.remote.PowerPayClient
import com.fordham.toolbelt.data.remote.PowerPayClientOutcome
import com.fordham.toolbelt.data.remote.PowerPayCreatePaymentRequestDto
import com.fordham.toolbelt.data.remote.PowerPayPaymentResponseDto
import com.fordham.toolbelt.data.remote.PowerPayWebhookEventDto
import com.fordham.toolbelt.util.randomUUID
import kotlinx.datetime.Clock

class MockPowerPayClient : PowerPayClient {
    private val payments = mutableListOf<PowerPayPaymentResponseDto>()
    private val events = mutableListOf<PowerPayWebhookEventDto>()

    override suspend fun createInvoicePayment(
        request: PowerPayCreatePaymentRequestDto
    ): PowerPayClientOutcome<PowerPayPaymentResponseDto> {
        val response = PowerPayPaymentResponseDto(
            paymentId = "mock-${randomUUID()}",
            invoiceId = request.invoiceId,
            clientName = request.clientName,
            amountUsd = request.amountUsd,
            requestType = request.requestType,
            provider = request.provider,
            status = "pending",
            paymentLinkUrl = "https://sandbox.powerpay.example/pay/${request.invoiceId}",
            assetCode = "USDC",
            receiptUrl = null,
            explorerUrl = null,
            createdAtMillis = Clock.System.now().toEpochMilliseconds()
        )
        payments.add(0, response)
        return PowerPayClientOutcome.Success(response)
    }

    override suspend fun getPaymentStatus(paymentId: String): PowerPayClientOutcome<PowerPayPaymentResponseDto> {
        val payment = payments.find { it.paymentId == paymentId }
            ?: return PowerPayClientOutcome.Failure(
                com.fordham.toolbelt.domain.model.FailureMessage("Mock payment not found.")
            )
        return PowerPayClientOutcome.Success(payment)
    }

    override suspend fun getTransactionHistory(): PowerPayClientOutcome<List<PowerPayPaymentResponseDto>> {
        return PowerPayClientOutcome.Success(payments.toList())
    }

    override suspend fun pollWebhookEvents(
        sinceEpochSeconds: Long
    ): PowerPayClientOutcome<List<PowerPayWebhookEventDto>> {
        return PowerPayClientOutcome.Success(events.filter { true })
    }

    /** Test helper: simulate client receiving invoice.paid (mirrors pay.on('invoice.paid')). */
    fun simulateInvoicePaid(
        invoiceId: String,
        receiptUrl: String = "https://sandbox.powerpay.example/receipt/$invoiceId",
        txHash: String = "mock-tx-${invoiceId.take(8)}"
    ) {
        val payment = payments.find { it.invoiceId == invoiceId }
        if (payment != null) {
            val index = payments.indexOf(payment)
            payments[index] = payment.copy(
                status = "paid",
                receiptUrl = receiptUrl,
                transactionHash = txHash,
                explorerUrl = "https://testnet.stellarchain.io/tx/$txHash"
            )
        }
        events.add(
            PowerPayWebhookEventDto(
                type = "invoice.paid",
                invoiceId = invoiceId,
                receiptUrl = receiptUrl,
                transactionHash = txHash,
                explorerUrl = "https://testnet.stellarchain.io/tx/$txHash",
                eventId = randomUUID()
            )
        )
    }
}
