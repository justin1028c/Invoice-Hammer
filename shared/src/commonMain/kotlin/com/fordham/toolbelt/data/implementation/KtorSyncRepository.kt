package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.domain.model.BackupFileName
import com.fordham.toolbelt.domain.model.BackupPayload
import com.fordham.toolbelt.domain.model.ClientListOutcome
import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.model.ReceiptListOutcome
import com.fordham.toolbelt.domain.model.SyncOutcome
import com.fordham.toolbelt.domain.model.SyncUploadOutcome
import com.fordham.toolbelt.domain.model.SupplierListOutcome
import com.fordham.toolbelt.domain.repository.ClientRepository
import com.fordham.toolbelt.domain.repository.DriveAuthTokenProvider
import com.fordham.toolbelt.domain.repository.DriveTokenOutcome
import com.fordham.toolbelt.domain.repository.InvoiceRepository
import com.fordham.toolbelt.domain.repository.ReceiptRepository
import com.fordham.toolbelt.domain.repository.SettingsRepository
import com.fordham.toolbelt.domain.repository.SyncRepository
import com.fordham.toolbelt.domain.repository.SupplierRepository
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.http.content.OutgoingContent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.json.*

/**
 * Responsibility: Handle cloud synchronization using Ktor. 
 * Replaces platform-specific implementations to ensure KMP compliance.
 */
class KtorSyncRepository(
    private val httpClient: HttpClient,
    private val driveAuthTokenProvider: DriveAuthTokenProvider,
    private val invoiceRepository: InvoiceRepository,
    private val receiptRepository: ReceiptRepository,
    private val clientRepository: ClientRepository,
    private val supplierRepository: SupplierRepository,
    private val settingsRepository: SettingsRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : SyncRepository {
    
    private val driveUploadUrl = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"

    override suspend fun syncInvoices(): SyncOutcome = withContext(ioDispatcher) {
        val backup = BackupPayload(buildBackupJson().toString().encodeToByteArray())
        when (val upload = uploadToDrive(BackupFileName("invoice-hammer-backup.json"), backup)) {
            is SyncUploadOutcome.Success -> SyncOutcome.Success
            is SyncUploadOutcome.Failure -> SyncOutcome.Failure(upload.error)
        }
    }

    override suspend fun syncReceipts(): SyncOutcome = withContext(ioDispatcher) {
        syncInvoices()
    }

    override suspend fun uploadToDrive(fileName: BackupFileName, content: BackupPayload): SyncUploadOutcome = withContext(ioDispatcher) {
        val token = when (val tokenOutcome = driveAuthTokenProvider.getDriveAccessToken()) {
            is DriveTokenOutcome.Success -> tokenOutcome.token.value
            is DriveTokenOutcome.Failure -> return@withContext SyncUploadOutcome.Failure(tokenOutcome.error)
        }

        return@withContext try {
            val boundary = "invoice-hammer-${Clock.System.now().toEpochMilliseconds()}"
            val body = buildMultipartBody(
                boundary = boundary,
                fileName = fileName.value,
                content = content.bytes
            )
            val multipartContentType = ContentType.parse("multipart/related; boundary=$boundary")

            val response = httpClient.post(driveUploadUrl) {
                header(HttpHeaders.Authorization, "Bearer $token")
                accept(ContentType.Application.Json)
                setBody(DriveMultipartContent(body, multipartContentType))
            }

            if (response.status.isSuccess()) {
                SyncUploadOutcome.Success("appDataFolder/${fileName.value}")
            } else {
                val responseBody = response.bodyAsText().trim()
                val details = responseBody.takeIf { it.isNotBlank() }?.let { " $it" }.orEmpty()
                SyncUploadOutcome.Failure(
                    FailureMessage("Drive upload failed with HTTP ${response.status.value}.$details")
                )
            }
        } catch (e: Exception) {
            SyncUploadOutcome.Failure(FailureMessage(e.message ?: "Failed to upload backup to Google Drive."))
        }
    }

    private suspend fun buildBackupJson(): JsonObject {
        val invoices = invoiceRepository.allInvoices.first()
        val receipts = when (val outcome = receiptRepository.allItems.first()) {
            is ReceiptListOutcome.Success -> outcome.receipts
            is ReceiptListOutcome.Failure -> emptyList()
        }
        val clients = when (val outcome = clientRepository.getAllClients().first()) {
            is ClientListOutcome.Success -> outcome.clients
            is ClientListOutcome.Failure -> emptyList()
        }
        val visibleSuppliers = when (val outcome = supplierRepository.getVisibleSuppliers().first()) {
            is SupplierListOutcome.Success -> outcome.suppliers
            is SupplierListOutcome.Failure -> emptyList()
        }
        val hiddenSuppliers = when (val outcome = supplierRepository.getHiddenSuppliers().first()) {
            is SupplierListOutcome.Success -> outcome.suppliers
            is SupplierListOutcome.Failure -> emptyList()
        }
        val settings = settingsRepository.getBusinessSettings()

        return buildJsonObject {
            put("schemaVersion", 1)
            put("exportedAtMillis", Clock.System.now().toEpochMilliseconds())
            putJsonObject("settings") {
                put("businessName", settings.businessName)
                put("businessSlogan", settings.businessSlogan)
                put("businessPhone", settings.businessPhone)
                put("businessEmail", settings.businessEmail)
                put("businessAddress", settings.businessAddress)
                put("taxRate", settings.taxRate)
                put("markupPercentage", settings.markupPercentage)
                put("isPremium", settings.isPremium)
                put("isDarkMode", settings.isDarkMode)
                put("useMetricUnits", settings.useMetricUnits)
                put("notificationsEnabled", settings.notificationsEnabled)
            }
            putJsonArray("clients") {
                clients.forEach { client ->
                    addJsonObject {
                        put("id", client.id.value)
                        put("name", client.name)
                        put("email", client.email.value)
                        put("phone", client.phone.value)
                        put("address", client.address)
                        put("notes", client.notes)
                        put("totalInvoiced", client.totalInvoiced)
                        put("isFavorite", client.isFavorite)
                        put("lastUpdated", client.lastUpdated)
                    }
                }
            }
            putJsonArray("invoices") {
                invoices.forEach { invoice ->
                    addJsonObject {
                        put("id", invoice.id.value)
                        put("clientName", invoice.clientName)
                        put("clientAddress", invoice.clientAddress)
                        put("clientPhone", invoice.clientPhone.value)
                        put("clientEmail", invoice.clientEmail.value)
                        put("date", invoice.date)
                        put("totalAmount", invoice.totalAmount)
                        put("depositAmount", invoice.depositAmount)
                        put("itemsSummary", invoice.itemsSummary)
                        put("pdfPath", invoice.pdfPath)
                        put("isPaid", invoice.isPaid)
                        put("isEstimate", invoice.isEstimate)
                        put("lastUpdated", invoice.lastUpdated)
                        put("durationSeconds", invoice.durationSeconds)
                    }
                }
            }
            putJsonArray("receipts") {
                receipts.forEach { receipt ->
                    addJsonObject {
                        put("id", receipt.id.value)
                        put("description", receipt.description)
                        put("quantity", receipt.quantity)
                        put("unitPrice", receipt.unitPrice)
                        put("totalPrice", receipt.totalPrice)
                        put("category", receipt.category)
                        put("clientName", receipt.clientName)
                        put("imagePath", receipt.imagePath)
                        put("isBilled", receipt.isBilled)
                        put("lastUpdated", receipt.lastUpdated)
                        put("supplierName", receipt.supplierName)
                        put("linkedInvoiceId", receipt.linkedInvoiceId?.value)
                    }
                }
            }
            putJsonArray("suppliers") {
                (visibleSuppliers + hiddenSuppliers).forEach { supplier ->
                    addJsonObject {
                        put("id", supplier.id.value)
                        put("name", supplier.name)
                        put("category", supplier.category.name)
                        put("address", supplier.address)
                        put("phone", supplier.phone.value)
                        put("webUrl", supplier.webUrl)
                        put("packageName", supplier.packageName)
                        put("displayOrder", supplier.displayOrder)
                        put("isPinned", supplier.isPinned)
                        put("isHidden", supplier.isHidden)
                        put("customLogoPath", supplier.customLogoPath)
                        put("logoResName", supplier.logoResName)
                        put("isDefault", supplier.isDefault)
                    }
                }
            }
        }
    }

    private fun buildMultipartBody(
        boundary: String,
        fileName: String,
        content: ByteArray
    ): ByteArray {
        val metadata = buildJsonObject {
            put("name", fileName)
            putJsonArray("parents") {
                add("appDataFolder")
            }
        }.toString()

        val prefix = buildString {
            append("--").append(boundary).append("\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n")
            append("\r\n")
            append(metadata).append("\r\n")
            append("--").append(boundary).append("\r\n")
            append("Content-Type: application/json\r\n")
            append("\r\n")
        }.encodeToByteArray()
        val suffix = "\r\n--$boundary--\r\n".encodeToByteArray()

        return prefix + content + suffix
    }

    private class DriveMultipartContent(
        private val bytes: ByteArray,
        override val contentType: ContentType
    ) : OutgoingContent.ByteArrayContent() {
        override val contentLength: Long = bytes.size.toLong()

        override fun bytes(): ByteArray = bytes
    }
}
