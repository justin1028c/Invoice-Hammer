package com.fordham.toolbelt.domain.repository

import com.fordham.toolbelt.domain.model.BackupFileName
import com.fordham.toolbelt.domain.model.BackupPayload
import com.fordham.toolbelt.domain.model.SyncOutcome
import com.fordham.toolbelt.domain.model.SyncUploadOutcome

interface SyncRepository {
    suspend fun syncInvoices(): SyncOutcome
    suspend fun syncReceipts(): SyncOutcome
    suspend fun uploadToDrive(fileName: BackupFileName, content: BackupPayload): SyncUploadOutcome
}
