package com.fordham.toolbelt.domain.repository

import com.fordham.toolbelt.domain.model.DraftInvoice
import kotlinx.coroutines.flow.Flow

interface DraftRepository {
    fun getDraft(): Flow<DraftInvoice>
    suspend fun saveDraft(draft: DraftInvoice)
    suspend fun clearDraft()
}
