package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.JobNote
import com.fordham.toolbelt.domain.model.JobNoteOutcome
import com.fordham.toolbelt.domain.model.NoteId
import com.fordham.toolbelt.domain.repository.JobNoteRepository
import com.fordham.toolbelt.util.randomUUID
import kotlinx.datetime.Clock

/**
 * Responsibility: Logic for creating and persisting a new job note.
 */
class AddJobNoteUseCase(
    private val repository: JobNoteRepository
) {
    suspend operator fun invoke(clientName: String, text: String): JobNoteOutcome {
        val note = JobNote(
            id = NoteId(randomUUID()),
            clientName = clientName,
            text = text,
            timestamp = Clock.System.now().toEpochMilliseconds()
        )
        return repository.insertNote(note)
    }
}
