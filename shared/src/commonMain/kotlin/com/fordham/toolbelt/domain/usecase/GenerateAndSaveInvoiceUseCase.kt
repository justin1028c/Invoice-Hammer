package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.model.agent.SystemBudgetSerializer
import com.fordham.toolbelt.domain.repository.*
import com.fordham.toolbelt.domain.repository.DocumentExporter
import com.fordham.toolbelt.util.DateTimeUtil
import com.fordham.toolbelt.util.randomUUID
import com.fordham.toolbelt.util.AppLogger
import kotlinx.datetime.Clock

data class GenerateInvoiceRequest(
    val clientName: ClientName,
    val clientAddress: ClientAddress,
    val saveToClientDirectory: Boolean,
    val taxRate: TaxRatePercent,
    val deposit: MoneyAmount,
    val lineItems: List<LineItem>,
    val logoUriString: MediaUri?,
    val businessSettings: BusinessSettings,
    val isEstimate: Boolean,
    val elapsedSeconds: DurationSeconds,
    val capturedPhotos: List<CapturedJobPhoto>,
    val linkedReceiptIds: List<ReceiptId>,
    val availableReceipts: List<ReceiptItem>,
    val onGenerated: (PdfFilePath) -> Unit
)

class GenerateAndSaveInvoiceUseCase(
    private val saveInvoiceUseCase: SaveInvoiceUseCase,
    private val clientRepository: ClientRepository,
    private val photoRepository: PhotoRepository,
    private val storageRepository: StorageRepository,
    private val receiptRepository: ReceiptRepository,
    private val engine: InvoiceEngine,
    private val documentExporter: DocumentExporter,
    private val jobNoteRepository: JobNoteRepository
) {
    suspend operator fun invoke(request: GenerateInvoiceRequest): GenerateInvoiceOutcome {
        return try {
            AppLogger.d(LOG_TAG, " Step 1 - Saving client if saveToClientDirectory=${request.saveToClientDirectory} is true...")
            // 1. Save to Client Directory if requested
            if (request.saveToClientDirectory && request.clientName.value.isNotEmpty()) {
                clientRepository.insertClient(
                    Client(
                        id = ClientId(randomUUID()),
                        name = request.clientName.value,
                        address = request.clientAddress.value
                    )
                )
            }

            AppLogger.d(LOG_TAG, " Step 2 - Normalizing logo and job photos for PDF...")
            val logoUri = request.logoUriString?.value ?: request.businessSettings.logoUri
            val normalizedLogoUri = normalizeMediaUri(logoUri)

            // Copy camera/content URIs to stable vault JPEGs before PDF embed (fixes HEIC + provider URIs).
            val normalizedPhotos = request.capturedPhotos.map { captured ->
                val vaultResult = storageRepository.saveUriToPictures(captured.uri, "JOB")
                val stableUri = if (vaultResult is StorageOutcome.Success) vaultResult.path else captured.uri
                captured.copy(uri = stableUri)
            }

            AppLogger.d(LOG_TAG, " Step 2b - Preparing data...")
            val date = DateTimeUtil.getNowFormatted()
            val subtotal = request.lineItems.sumOf { it.amount }
            val invoiceId = randomUUID()

            val data = InvoiceData(
                invoiceId = invoiceId,
                clientName = request.clientName.value,
                clientAddress = request.clientAddress.value,
                items = request.lineItems,
                taxRate = request.taxRate.value,
                date = date,
                logoUriString = normalizedLogoUri,
                settings = request.businessSettings,
                isEstimate = request.isEstimate,
                deposit = request.deposit.value,
                jobSitePhotos = normalizedPhotos
            )

            AppLogger.d(LOG_TAG, " Step 3 - Generating PDF...")
            // 3. Generate PDF
            val workingPath = engine.generatePdf(data)
            if (workingPath == null) {
                AppLogger.e(LOG_TAG, " PDF generation FAILED!")
                return GenerateInvoiceOutcome.Error(
                    GenerateInvoiceFailure.PdfGenerationFailure(FailureMessage("Failed to generate PDF"))
                )
            }
            AppLogger.d(LOG_TAG, " PDF generated successfully at $workingPath")

            // 3a. Publish a user-visible copy under Documents/InvoiceHammer/Invoices/.
            // We always keep `path` (used by FileProvider share + DB) as the shareable
            // working location so existing share/print flows are unchanged. Publishing
            // is best-effort: a failure here must not block invoice save.
            val displayName = "${if (data.isEstimate) "Estimate" else "Invoice"}_${data.invoiceId}.pdf"
            val publishOutcome = documentExporter.publish(workingPath, DocumentCategory.Invoices, displayName)
            val path = when (publishOutcome) {
                is DocumentExportOutcome.Success -> {
                    AppLogger.d(LOG_TAG, " Mirrored to user-visible ${publishOutcome.location.userVisiblePath}")
                    publishOutcome.location.shareablePath
                }
                is DocumentExportOutcome.Failure -> {
                    AppLogger.e(LOG_TAG, " Publish to Documents/InvoiceHammer failed: ${publishOutcome.error.value}")
                    workingPath
                }
            }

            AppLogger.d(LOG_TAG, " Step 4 - Saving Invoice to Database...")
            // 4. Save Invoice to Database
            val saveResult = saveInvoiceUseCase(
                SaveInvoiceRequest(
                    clientName = request.clientName,
                    clientAddress = request.clientAddress,
                    subtotal = MoneyAmount(subtotal),
                    taxRate = request.taxRate,
                    depositAmount = request.deposit,
                    itemsSummary = ItemsSummary(request.lineItems.joinToString { it.description }),
                    pdfPath = PdfFilePath(path),
                    isEstimate = request.isEstimate,
                    durationSeconds = request.elapsedSeconds
                )
            )

            if (saveResult is SaveInvoiceOutcome.Success) {
                val savedInvoice = saveResult.invoice
                AppLogger.d(LOG_TAG, " Invoice saved to DB successfully with ID: ${savedInvoice.id}")

                // Save system budget note for Profit Guardian variance tracking
                val materialBudget = request.lineItems.filter { it.category.equals("Materials", ignoreCase = true) }.sumOf { it.amount }
                val systemNoteText = SystemBudgetSerializer.serialize(
                    revenue = savedInvoice.totalAmount,
                    materials = materialBudget,
                    lineItems = request.lineItems
                )
                try {
                    jobNoteRepository.insertNote(
                        JobNote(
                            id = NoteId(randomUUID()),
                            clientName = savedInvoice.clientName,
                            invoiceId = savedInvoice.id,
                            text = systemNoteText,
                            timestamp = Clock.System.now().toEpochMilliseconds()
                        )
                    )
                    AppLogger.d(LOG_TAG, " System budget note saved successfully for estimate/invoice.")
                } catch (e: Exception) {
                    AppLogger.e(LOG_TAG, " Failed to save system budget note", e)
                }

                AppLogger.d(LOG_TAG, " Step 5 - Persisting Linked Receipts...")
                // 5. Persist Linked Receipts
                request.linkedReceiptIds.forEach { receiptId ->
                    val receipt = request.availableReceipts.find { it.id == receiptId }
                    if (receipt != null) {
                        receiptRepository.updateItem(receipt.copy(
                            linkedInvoiceId = savedInvoice.id,
                            isBilled = true,
                            clientName = savedInvoice.clientName
                        ))
                    }
                }

                AppLogger.d(LOG_TAG, " Step 6 - Saving Job Photos...")
                normalizedPhotos.forEach { captured ->
                    photoRepository.savePhoto(
                        JobPhoto(
                            id = PhotoId(randomUUID()),
                            invoiceId = savedInvoice.id,
                            localUri = captured.uri,
                            phase = captured.phase,
                            timestamp = Clock.System.now().toEpochMilliseconds()
                        )
                    )
                }

                AppLogger.d(LOG_TAG, " Step 7 - Triggering onGenerated callback...")
                request.onGenerated(PdfFilePath(path))
                AppLogger.d(LOG_TAG, " Done!")
                GenerateInvoiceOutcome.Success
            } else if (saveResult is SaveInvoiceOutcome.Error) {
                AppLogger.e(LOG_TAG, " SaveInvoiceUseCase FAILED: ${saveResult.message}")
                GenerateInvoiceOutcome.Error(
                    GenerateInvoiceFailure.PersistenceFailure(saveResult.failure.message)
                )
            } else {
                AppLogger.e(LOG_TAG, " SaveInvoiceUseCase returned unknown state")
                GenerateInvoiceOutcome.Error(
                    GenerateInvoiceFailure.UnexpectedFailure(FailureMessage("Unknown error saving invoice"))
                )
            }
        } catch (e: Exception) {
            AppLogger.e(LOG_TAG, "Exception caught: ${e.message}", e)
            GenerateInvoiceOutcome.Error(
                GenerateInvoiceFailure.UnexpectedFailure(
                    FailureMessage("Error saving ${if (request.isEstimate) "estimate" else "invoice"}: ${e.message}")
                )
            )
        }
    }

    private suspend fun normalizeMediaUri(uri: String?): String? {
        if (uri.isNullOrBlank()) return null
        return when (val outcome = storageRepository.saveUriToPictures(uri, "MEDIA")) {
            is StorageOutcome.Success -> outcome.path
            is StorageOutcome.Failure -> uri
        }
    }
}

private const val LOG_TAG = "GenerateAndSaveInvoice"
