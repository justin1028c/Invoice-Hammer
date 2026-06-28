package com.fordham.toolbelt.domain.repository

import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.InvoiceId
import com.fordham.toolbelt.domain.model.PaymentLedgerOutcome
import com.fordham.toolbelt.domain.model.PaymentProviderType
import com.fordham.toolbelt.domain.model.PaymentRequestOutcome
import com.fordham.toolbelt.domain.model.PaymentRequestType
import com.fordham.toolbelt.domain.model.cardterminal.CardBrand
import com.fordham.toolbelt.domain.model.cardterminal.CardTerminalPaymentOutcome
import kotlinx.coroutines.flow.Flow

interface PaymentRepository {
    val ledger: Flow<PaymentLedgerOutcome>
    suspend fun createPaymentRequest(invoice: Invoice, type: PaymentRequestType, provider: PaymentProviderType): PaymentRequestOutcome
    suspend fun refreshLedger(): PaymentLedgerOutcome
    suspend fun markInvoicePaid(
        invoiceId: InvoiceId,
        paidAtMillis: Long
    ): PaymentLedgerOutcome

    suspend fun recordCardTerminalPayment(
        invoice: Invoice,
        type: PaymentRequestType,
        amount: Double,
        lastFourDigits: String,
        brand: CardBrand,
        paidAtMillis: Long
    ): CardTerminalPaymentOutcome
}
