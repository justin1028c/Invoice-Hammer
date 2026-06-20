package com.fordham.toolbelt.domain.repository

import com.fordham.toolbelt.domain.model.JobNote
import com.fordham.toolbelt.domain.model.JobNoteOutcome
import com.fordham.toolbelt.domain.model.InvoiceId
import kotlinx.coroutines.flow.Flow

interface JobNoteRepository {
    fun getNotesByInvoice(invoiceId: InvoiceId): Flow<List<JobNote>>
    fun getNotesByClient(clientName: String): Flow<List<JobNote>>
    suspend fun insertNote(note: JobNote): JobNoteOutcome
    suspend fun deleteNote(note: JobNote): JobNoteOutcome
    suspend fun deleteAllNotes(): JobNoteOutcome
}