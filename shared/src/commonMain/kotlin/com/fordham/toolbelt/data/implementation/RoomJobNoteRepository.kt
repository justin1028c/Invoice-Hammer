package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.JobNoteDao
import com.fordham.toolbelt.data.toDomain
import com.fordham.toolbelt.data.toEntity
import com.fordham.toolbelt.domain.model.JobNote
import com.fordham.toolbelt.domain.model.JobNoteOutcome
import com.fordham.toolbelt.domain.model.InvoiceId
import com.fordham.toolbelt.domain.repository.JobNoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomJobNoteRepository(
    private val jobNoteDao: JobNoteDao
) : JobNoteRepository {
    override fun getNotesByInvoice(invoiceId: InvoiceId): Flow<List<JobNote>> =
        jobNoteDao.getNotesByInvoice(invoiceId.value).map { list -> list.map { it.toDomain() } }

    override fun getNotesByClient(clientName: String): Flow<List<JobNote>> =
        jobNoteDao.getNotesByClient(clientName).map { list -> list.map { it.toDomain() } }

    override suspend fun insertNote(note: JobNote): JobNoteOutcome = try {
        jobNoteDao.insertNote(note.toEntity())
        JobNoteOutcome.Success
    } catch (e: Exception) {
        logRepositoryFailure("RoomJobNoteRepository", "repository", e)
        JobNoteOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to insert note"))
    }

    override suspend fun deleteNote(note: JobNote): JobNoteOutcome = try {
        jobNoteDao.deleteNote(note.toEntity())
        JobNoteOutcome.Success
    } catch (e: Exception) {
        logRepositoryFailure("RoomJobNoteRepository", "repository", e)
        JobNoteOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to delete note"))
    }

    override suspend fun deleteAllNotes(): JobNoteOutcome = try {
        jobNoteDao.deleteAllNotes()
        JobNoteOutcome.Success
    } catch (e: Exception) {
        logRepositoryFailure("RoomJobNoteRepository", "repository", e)
        JobNoteOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to delete all notes"))
    }
}

private const val TAG = "RoomJobNoteRepository"
