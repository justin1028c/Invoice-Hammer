package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.domain.model.BackupFileName
import com.fordham.toolbelt.domain.model.BackupPayload
import com.fordham.toolbelt.domain.model.ClientListOutcome
import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.model.ReceiptListOutcome
import com.fordham.toolbelt.domain.model.SyncOutcome
import com.fordham.toolbelt.domain.model.SyncUploadOutcome
import com.fordham.toolbelt.domain.model.SupplierListOutcome
import com.fordham.toolbelt.data.remote.SupabaseBackupDownloadOutcome
import com.fordham.toolbelt.data.remote.SupabaseBackupUploadOutcome
import com.fordham.toolbelt.data.remote.SupabaseBackupUploadRequest
import com.fordham.toolbelt.data.remote.SupabaseClient
import com.fordham.toolbelt.data.remote.SupabaseConfig
import com.fordham.toolbelt.domain.model.ClientOutcome
import com.fordham.toolbelt.domain.model.InvoiceOutcome
import com.fordham.toolbelt.domain.model.ReceiptOutcome
import com.fordham.toolbelt.domain.model.SettingsOutcome
import com.fordham.toolbelt.domain.model.SupplierOutcome
import com.fordham.toolbelt.domain.repository.AuthRepository
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
    private val supabaseClient: SupabaseClient,
    private val supabaseConfig: SupabaseConfig,
    private val authRepository: AuthRepository,
    private val invoiceRepository: InvoiceRepository,
    private val receiptRepository: ReceiptRepository,
    private val clientRepository: ClientRepository,
    private val supplierRepository: SupplierRepository,
    private val settingsRepository: SettingsRepository,
    private val ioDispatcher: CoroutineDispatcher
) : SyncRepository {
    
    private val driveUploadUrl = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"

    override suspend fun syncInvoices(): SyncOutcome = withContext(ioDispatcher) {
        val backupJson = KtorSyncBackupJsonBuilder.build(
            invoiceRepository = invoiceRepository,
            receiptRepository = receiptRepository,
            clientRepository = clientRepository,
            supplierRepository = supplierRepository,
            settingsRepository = settingsRepository
        )
        val backup = BackupPayload(backupJson.toString().encodeToByteArray())
        val driveOutcome = uploadToDrive(BackupFileName("invoice-hammer-backup.json"), backup)
        val supabaseOutcome = uploadToSupabase(backupJson)

        val driveOk = driveOutcome is SyncUploadOutcome.Success
        val supabaseOk = supabaseOutcome is SupabaseBackupUploadOutcome.Success

        return@withContext when {
            driveOk || supabaseOk -> SyncOutcome.Success
            supabaseOutcome is SupabaseBackupUploadOutcome.Skipped ->
                when (driveOutcome) {
                    is SyncUploadOutcome.Success -> SyncOutcome.Success
                    is SyncUploadOutcome.Failure -> SyncOutcome.Failure(driveOutcome.error)
                }
            supabaseOutcome is SupabaseBackupUploadOutcome.Failure &&
                driveOutcome is SyncUploadOutcome.Failure ->
                SyncOutcome.Failure(
                    FailureMessage(
                        "Drive: ${driveOutcome.error.value} · Supabase: ${supabaseOutcome.error.value}"
                    )
                )
            supabaseOutcome is SupabaseBackupUploadOutcome.Failure ->
                SyncOutcome.Failure(supabaseOutcome.error)
            else ->
                when (driveOutcome) {
                    is SyncUploadOutcome.Success -> SyncOutcome.Success
                    is SyncUploadOutcome.Failure -> SyncOutcome.Failure(driveOutcome.error)
                }
        }
    }

    private suspend fun uploadToSupabase(backupJson: JsonObject): SupabaseBackupUploadOutcome {
        if (!supabaseConfig.isConfigured) {
            return SupabaseBackupUploadOutcome.Skipped
        }

        val user = authRepository.currentUser.first()
            ?: return SupabaseBackupUploadOutcome.Failure(
                FailureMessage("Sign in to back up to Supabase.")
            )

        return supabaseClient.uploadBackup(
            SupabaseBackupUploadRequest(
                userId = user.id.value,
                backupJson = backupJson,
                exportedAtMillis = Clock.System.now().toEpochMilliseconds(),
                schemaVersion = 1
            )
        )
    }

    override suspend fun syncReceipts(): SyncOutcome = withContext(ioDispatcher) {
        syncInvoices()
    }

    override suspend fun restoreFromDrive(): SyncOutcome = withContext(ioDispatcher) {
        val token = when (val tokenOutcome = driveAuthTokenProvider.getDriveAccessToken()) {
            is DriveTokenOutcome.Success -> tokenOutcome.token.value
            is DriveTokenOutcome.Failure -> return@withContext SyncOutcome.Failure(
                FailureMessage("Drive sign-in required: ${tokenOutcome.error.value}")
            )
        }

        return@withContext try {
            // List files in appDataFolder to find the backup
            val listUrl = "https://www.googleapis.com/drive/v3/files" +
                "?spaces=appDataFolder" +
                "&fields=files(id,name)" +
                "&q=name%3D%27invoice-hammer-backup.json%27"

            val listResponse = httpClient.get(listUrl) {
                header(HttpHeaders.Authorization, "Bearer $token")
                accept(ContentType.Application.Json)
            }

            if (!listResponse.status.isSuccess()) {
                return@withContext SyncOutcome.Failure(
                    FailureMessage("Drive listing failed with HTTP ${listResponse.status.value}.")
                )
            }

            val listJson = Json.parseToJsonElement(listResponse.bodyAsText()).jsonObject
            val files = listJson["files"]?.jsonArray
            if (files.isNullOrEmpty()) {
                return@withContext SyncOutcome.Failure(
                    FailureMessage("No Drive backup found. Run SYNC NOW first.")
                )
            }

            val fileId = files[0].jsonObject["id"]?.jsonPrimitive?.content
                ?: return@withContext SyncOutcome.Failure(
                    FailureMessage("Drive backup file ID missing.")
                )

            // Download the file content
            val downloadUrl = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
            val downloadResponse = httpClient.get(downloadUrl) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            if (!downloadResponse.status.isSuccess()) {
                return@withContext SyncOutcome.Failure(
                    FailureMessage("Drive download failed with HTTP ${downloadResponse.status.value}.")
                )
            }

            val backupJson = Json.parseToJsonElement(downloadResponse.bodyAsText()).jsonObject
            applyBackupSnapshot(backupJson)
        } catch (e: Exception) {
            SyncOutcome.Failure(FailureMessage(e.message ?: "Failed to restore from Google Drive."))
        }
    }

    override suspend fun restoreLatest(): SyncOutcome = withContext(ioDispatcher) {
        // Drive is the primary sync target (syncInvoices always writes there).
        // Attempt Drive restore first; only fall back to Supabase if Drive
        // cannot supply a backup (no token, no file, or network failure).
        val driveOutcome = restoreFromDrive()
        if (driveOutcome is SyncOutcome.Success) return@withContext driveOutcome

        // Supabase fallback — only if it is configured.
        if (!supabaseConfig.isConfigured) return@withContext driveOutcome
        restoreFromSupabase()
    }

    override suspend fun restoreFromSupabase(): SyncOutcome = withContext(ioDispatcher) {
        if (!supabaseConfig.isConfigured) {
            return@withContext SyncOutcome.Failure(FailureMessage("Supabase is not configured."))
        }

        val user = authRepository.currentUser.first()
            ?: return@withContext SyncOutcome.Failure(FailureMessage("Sign in to restore from Supabase."))

        when (val download = supabaseClient.downloadLatestBackup(user.id.value)) {
            SupabaseBackupDownloadOutcome.NotConfigured ->
                SyncOutcome.Failure(FailureMessage("Supabase is not configured."))
            SupabaseBackupDownloadOutcome.NotFound ->
                SyncOutcome.Failure(FailureMessage("No Supabase backup found for this account. Run SYNC NOW first."))
            is SupabaseBackupDownloadOutcome.Failure ->
                SyncOutcome.Failure(download.error)
            is SupabaseBackupDownloadOutcome.Success ->
                applyBackupSnapshot(download.backupJson)
        }
    }

    private suspend fun applyBackupSnapshot(backupJson: JsonObject): SyncOutcome {
        val preserveLogoUri = settingsRepository.getBusinessSettings().logoUri
        val parsed = SupabaseBackupRestoreMapper.parse(backupJson, preserveLogoUri)

        when (val outcome = settingsRepository.saveBusinessSettings(parsed.settings)) {
            is SettingsOutcome.Failure -> return SyncOutcome.Failure(outcome.error)
            SettingsOutcome.Success -> Unit
        }

        when (val outcome = clientRepository.replaceAllClients(parsed.clients)) {
            is ClientOutcome.Failure -> return SyncOutcome.Failure(outcome.error)
            ClientOutcome.Success -> Unit
        }

        when (val outcome = supplierRepository.replaceAllSuppliers(parsed.suppliers)) {
            is SupplierOutcome.Failure -> return SyncOutcome.Failure(outcome.error)
            SupplierOutcome.Success -> Unit
        }

        when (val outcome = invoiceRepository.deleteAllInvoices()) {
            is InvoiceOutcome.Failure -> return SyncOutcome.Failure(outcome.error)
            InvoiceOutcome.Success -> Unit
        }
        if (parsed.invoices.isNotEmpty()) {
            when (val outcome = invoiceRepository.insertInvoices(parsed.invoices)) {
                is InvoiceOutcome.Failure -> return SyncOutcome.Failure(outcome.error)
                InvoiceOutcome.Success -> Unit
            }
        }

        when (val outcome = receiptRepository.deleteAllItems()) {
            is ReceiptOutcome.Failure -> return SyncOutcome.Failure(outcome.error)
            ReceiptOutcome.Success -> Unit
        }
        if (parsed.receipts.isNotEmpty()) {
            when (val outcome = receiptRepository.insertItems(parsed.receipts)) {
                is ReceiptOutcome.Failure -> return SyncOutcome.Failure(outcome.error)
                ReceiptOutcome.Success -> Unit
            }
        }

        return SyncOutcome.Success
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
