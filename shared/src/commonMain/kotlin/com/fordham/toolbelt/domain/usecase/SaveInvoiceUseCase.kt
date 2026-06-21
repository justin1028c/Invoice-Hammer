package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.repository.InvoiceRepository
import com.fordham.toolbelt.util.AppLogger
import com.fordham.toolbelt.util.randomUUID
import kotlinx.datetime.Clock

data class SaveInvoiceRequest(
    val clientName: ClientName,
    val clientAddress: ClientAddress,
    val subtotal: MoneyAmount,
    val taxRate: TaxRatePercent,
    val depositAmount: MoneyAmount,
    val itemsSummary: ItemsSummary,
    val pdfPath: PdfFilePath,
    val isEstimate: Boolean,
    val durationSeconds: DurationSeconds = DurationSeconds(0L)
)

class SaveInvoiceUseCase(
    private val repository: InvoiceRepository
) {
    suspend operator fun invoke(request: SaveInvoiceRequest): SaveInvoiceOutcome {
        val total = (request.subtotal.value * (1 + request.taxRate.value / 100)) - request.depositAmount.value
        val invoice = Invoice(
            id = InvoiceId(randomUUID()),
            clientName = request.clientName,
            clientAddress = request.clientAddress,
            clientPhone = PhoneNumber(""),
            clientEmail = EmailAddress(""),
            date = com.fordham.toolbelt.util.DateTimeUtil.getNowFormatted(),
            totalAmount = MoneyAmount(total),
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
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            AppLogger.e("SaveInvoiceUseCase", "invoke failed", e)
            SaveInvoiceOutcome.Error(
                SaveInvoiceFailure.UnexpectedFailure(
                    FailureMessage(e.message ?: "Failed to save invoice")
                )
            )
        }
    }
}
