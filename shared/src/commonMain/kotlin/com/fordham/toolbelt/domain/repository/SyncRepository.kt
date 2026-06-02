package com.fordham.toolbelt.domain.repository

import com.fordham.toolbelt.domain.model.BackupFileName
import com.fordham.toolbelt.domain.model.BackupPayload
import com.fordham.toolbelt.domain.model.SyncOutcome
import com.fordham.toolbelt.domain.model.SyncUploadOutcome

interface SyncRepository {
    suspend fun syncInvoices(): SyncOutcome
    suspend fun syncReceipts(): SyncOutcome
    /** Restore from Google Drive appDataFolder backup. */
    suspend fun restoreFromDrive(): SyncOutcome
    /** Restore from Supabase cloud backup. */
    suspend fun restoreFromSupabase(): SyncOutcome
    /**
     * Smart restore: attempts Drive first (the primary sync target), then falls
     * back to Supabase if Drive is unavailable or has no backup. This is the
     * correct entry point for all user-facing "Restore" actions.
     */
    suspend fun restoreLatest(): SyncOutcome
    suspend fun uploadToDrive(fileName: BackupFileName, content: BackupPayload): SyncUploadOutcome
}
