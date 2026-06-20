package com.fordham.toolbelt.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface JobNoteDao {
    @Query("SELECT * FROM job_notes WHERE invoiceId = :invoiceId ORDER BY timestamp DESC")
    fun getNotesByInvoice(invoiceId: String): Flow<List<JobNoteEntity>>

    @Query("SELECT * FROM job_notes WHERE LOWER(clientName) = LOWER(:clientName) ORDER BY timestamp DESC")
    fun getNotesByClient(clientName: String): Flow<List<JobNoteEntity>>

    @Query("""
        SELECT * FROM job_notes 
        WHERE text LIKE '%' || :query || '%'
        ORDER BY timestamp DESC LIMIT 5
    """)
    suspend fun getRelevantContext(query: String): List<JobNoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: JobNoteEntity)

    @Delete
    suspend fun deleteNote(note: JobNoteEntity)

    @Query("DELETE FROM job_notes")
    suspend fun deleteAllNotes()
}
