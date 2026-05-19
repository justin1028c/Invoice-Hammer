package com.fordham.toolbelt.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DraftDao {
    @Query("SELECT * FROM DraftInvoiceEntity WHERE id = 'current_draft'")
    fun getDraft(): Flow<DraftInvoiceEntity?>

    @Upsert
    suspend fun saveDraft(draft: DraftInvoiceEntity)

    @Query("DELETE FROM DraftInvoiceEntity WHERE id = 'current_draft'")
    suspend fun clearDraft()
}
