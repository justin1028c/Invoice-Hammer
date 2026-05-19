package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.repository.InvoiceRepository
import com.fordham.toolbelt.util.randomUUID
import kotlinx.datetime.Clock

data class SaveInvoiceRequest(
    val clientName: String,
    val clientAddress: String,
    val subtotal: Double,
    val taxRate: Double,
    val depositAmount: Double,
    val itemsSummary: String,
    val pdfPath: String,
    val isEstimate: Boolean,
    val durationSeconds: Long = 0L
)

class SaveInvoiceUseCase(
    private val repository: InvoiceRepository
) {
    suspend operator fun invoke(request: SaveInvoiceRequest): SaveInvoiceOutcome {
        val total = (request.subtotal * (1 + request.taxRate / 100)) - request.depositAmount
        val invoice = Invoice(
            id = InvoiceId(randomUUID()),
            clientName = request.clientName,
            clientAddress = request.clientAddress,
            clientPhone = PhoneNumber(""), // Optional
            clientEmail = EmailAddress(""), // Optional
            date = com.fordham.toolbelt.util.DateTimeUtil.getNowFormatted(),
            totalAmount = total,
            depositAmount = request.depositAmount,
            itemsSummary = request.itemsSummary,
            pdfPath = request.pdfPath,
            isPaid = false,
            isEstimate = request.isEstimate,
            lastUpdated = Clock.System.now().toEpochMilliseconds(),
            durationSeconds = request.durationSeconds
        )
        return try {
            val result = repository.insertInvoice(invoice)
            if (result is InvoiceOutcome.Success) {
                SaveInvoiceOutcome.Success(invoice)
            } else {
                val errorMsg = (result as? InvoiceOutcome.Failure)?.error?.value ?: "Failed to save invoice"
                SaveInvoiceOutcome.Error(
                    SaveInvoiceFailure.PersistenceFailure(FailureMessage(errorMsg))
                )
            }
        } catch (e: Exception) {
            SaveInvoiceOutcome.Error(
                SaveInvoiceFailure.UnexpectedFailure(
                    FailureMessage(e.message ?: "Failed to save invoice")
                )
            )
        }
    }
}
