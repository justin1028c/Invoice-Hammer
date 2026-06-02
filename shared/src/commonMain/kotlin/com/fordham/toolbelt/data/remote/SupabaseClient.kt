package com.fordham.toolbelt.data.remote

import com.fordham.toolbelt.domain.model.FailureMessage
import kotlinx.serialization.json.JsonObject

interface SupabaseClient {
    suspend fun checkConnection(): SupabaseClientOutcome
    suspend fun uploadBackup(request: SupabaseBackupUploadRequest): SupabaseBackupUploadOutcome
    suspend fun downloadLatestBackup(userId: String): SupabaseBackupDownloadOutcome
}

sealed interface SupabaseClientOutcome {
    data object Connected : SupabaseClientOutcome
    data object NotConfigured : SupabaseClientOutcome
    data class Failure(val error: FailureMessage) : SupabaseClientOutcome
}

sealed interface SupabaseBackupUploadOutcome {
    data object Success : SupabaseBackupUploadOutcome
    data object Skipped : SupabaseBackupUploadOutcome
    data class Failure(val error: FailureMessage) : SupabaseBackupUploadOutcome
}

sealed interface SupabaseBackupDownloadOutcome {
    data class Success(
        val backupJson: JsonObject,
        val exportedAtMillis: Long?
    ) : SupabaseBackupDownloadOutcome

    data object NotConfigured : SupabaseBackupDownloadOutcome
    data object NotFound : SupabaseBackupDownloadOutcome
    data class Failure(val error: FailureMessage) : SupabaseBackupDownloadOutcome
}
