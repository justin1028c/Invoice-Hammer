package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.repository.*
import com.fordham.toolbelt.domain.repository.DocumentExporter
import com.fordham.toolbelt.util.DateTimeUtil
import com.fordham.toolbelt.util.randomUUID
import kotlinx.datetime.Clock

data class GenerateInvoiceRequest(
    val clientName: String,
    val clientAddress: String,
    val saveToClientDirectory: Boolean,
    val taxRate: Double,
    val deposit: Double,
    val lineItems: List<LineItem>,
    val logoUriString: String?,
    val businessSettings: BusinessSettings,
    val isEstimate: Boolean,
    val elapsedSeconds: Long,
    val capturedPhotos: List<String>,
    val linkedReceiptIds: List<String>,
    val availableReceipts: List<ReceiptItem>,
    val onGenerated: (String) -> Unit
)

class GenerateAndSaveInvoiceUseCase(
    private val saveInvoiceUseCase: SaveInvoiceUseCase,
    private val clientRepository: ClientRepository,
    private val photoRepository: PhotoRepository,
    private val storageRepository: StorageRepository,
    private val receiptRepository: ReceiptRepository,
    private val engine: InvoiceEngine,
    private val documentExporter: DocumentExporter
) {
    suspend operator fun invoke(request: GenerateInvoiceRequest): GenerateInvoiceOutcome {
        return try {
            println("GENERATE_SAVE_USECASE: Step 1 - Saving client if saveToClientDirectory=${request.saveToClientDirectory} is true...")
            // 1. Save to Client Directory if requested
            if (request.saveToClientDirectory && request.clientName.isNotEmpty()) {
                clientRepository.insertClient(Client(id = ClientId(randomUUID()), name = request.clientName, address = request.clientAddress))
            }

            println("GENERATE_SAVE_USECASE: Step 2 - Preparing data...")
            // 2. Prepare Data
            val date = DateTimeUtil.getNowFormatted()
            val subtotal = request.lineItems.sumOf { it.amount }
            val invoiceId = randomUUID()
            
            val data = InvoiceData(
                invoiceId = invoiceId,
                clientName = request.clientName,
                clientAddress = request.clientAddress,
                items = request.lineItems,
                taxRate = request.taxRate,
                date = date, 
                logoUriString = request.logoUriString,
                settings = request.businessSettings,
                isEstimate = request.isEstimate,
                deposit = request.deposit,
                photoUris = request.capturedPhotos
            )

            println("GENERATE_SAVE_USECASE: Step 3 - Generating PDF...")
            // 3. Generate PDF
            val workingPath = engine.generatePdf(data)
            if (workingPath == null) {
                println("GENERATE_SAVE_USECASE: PDF generation FAILED!")
                return GenerateInvoiceOutcome.Error(
                    GenerateInvoiceFailure.PdfGenerationFailure(FailureMessage("Failed to generate PDF"))
                )
            }
            println("GENERATE_SAVE_USECASE: PDF generated successfully at $workingPath")

            // 3a. Publish a user-visible copy under Documents/InvoiceHammer/Invoices/.
            // We always keep `path` (used by FileProvider share + DB) as the shareable
            // working location so existing share/print flows are unchanged. Publishing
            // is best-effort: a failure here must not block invoice save.
            val displayName = "${if (data.isEstimate) "Estimate" else "Invoice"}_${data.invoiceId}.pdf"
            val publishOutcome = documentExporter.publish(workingPath, DocumentCategory.Invoices, displayName)
            val path = when (publishOutcome) {
                is DocumentExportOutcome.Success -> {
                    println("GENERATE_SAVE_USECASE: Mirrored to user-visible ${publishOutcome.location.userVisiblePath}")
                    publishOutcome.location.shareablePath
                }
                is DocumentExportOutcome.Failure -> {
                    println("GENERATE_SAVE_USECASE: Publish to Documents/InvoiceHammer failed: ${publishOutcome.error.value}")
                    workingPath
                }
            }

            println("GENERATE_SAVE_USECASE: Step 4 - Saving Invoice to Database...")
            // 4. Save Invoice to Database
            val saveResult = saveInvoiceUseCase(
                SaveInvoiceRequest(
                    clientName = request.clientName,
                    clientAddress = request.clientAddress,
                    subtotal = subtotal,
                    taxRate = request.taxRate,
                    depositAmount = request.deposit,
                    itemsSummary = request.lineItems.joinToString { it.description },
                    pdfPath = path,
                    isEstimate = request.isEstimate,
                    durationSeconds = request.elapsedSeconds
                )
            )

            if (saveResult is SaveInvoiceOutcome.Success) {
                val savedInvoice = saveResult.invoice
                println("GENERATE_SAVE_USECASE: Invoice saved to DB successfully with ID: ${savedInvoice.id}")
                
                println("GENERATE_SAVE_USECASE: Step 5 - Persisting Linked Receipts...")
                // 5. Persist Linked Receipts
                request.linkedReceiptIds.forEach { receiptId ->
                    val receipt = request.availableReceipts.find { it.id.value == receiptId }
                    if (receipt != null) {
                        receiptRepository.updateItem(receipt.copy(
                            linkedInvoiceId = savedInvoice.id,
                            isBilled = true,
                            clientName = savedInvoice.clientName
                        ))
                    }
                }

                println("GENERATE_SAVE_USECASE: Step 6 - Saving Job Photos...")
                // 6. Save Photos
                request.capturedPhotos.forEach { photoUri ->
                    val vaultResult = storageRepository.saveUriToPictures(photoUri, "JOB")
                    val finalUri = if (vaultResult is StorageOutcome.Success) vaultResult.path else photoUri
                    
                    photoRepository.savePhoto(
                        JobPhoto(
                            id = PhotoId(randomUUID()),
                            invoiceId = savedInvoice.id,
                            localUri = finalUri,
                            timestamp = Clock.System.now().toEpochMilliseconds()
                        )
                    )
                }

                println("GENERATE_SAVE_USECASE: Step 7 - Triggering onGenerated callback...")
                request.onGenerated(path)
                println("GENERATE_SAVE_USECASE: Done!")
                GenerateInvoiceOutcome.Success
            } else if (saveResult is SaveInvoiceOutcome.Error) {
                println("GENERATE_SAVE_USECASE: SaveInvoiceUseCase FAILED: ${saveResult.message}")
                GenerateInvoiceOutcome.Error(
                    GenerateInvoiceFailure.PersistenceFailure(saveResult.failure.message)
                )
            } else {
                println("GENERATE_SAVE_USECASE: SaveInvoiceUseCase returned unknown state")
                GenerateInvoiceOutcome.Error(
                    GenerateInvoiceFailure.UnexpectedFailure(FailureMessage("Unknown error saving invoice"))
                )
            }
        } catch (e: Exception) {
            println("GENERATE_SAVE_USECASE: Exception caught: ${e.message}")
            e.printStackTrace()
            GenerateInvoiceOutcome.Error(
                GenerateInvoiceFailure.UnexpectedFailure(
                    FailureMessage("Error saving ${if (request.isEstimate) "estimate" else "invoice"}: ${e.message}")
                )
            )
        }
    }
}
