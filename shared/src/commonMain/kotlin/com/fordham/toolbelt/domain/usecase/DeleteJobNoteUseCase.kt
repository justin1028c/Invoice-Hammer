package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.JobNote
import com.fordham.toolbelt.domain.model.JobNoteOutcome
import com.fordham.toolbelt.domain.repository.JobNoteRepository

/**
 * Responsibility: Logic for deleting a job note.
 */
class DeleteJobNoteUseCase(
    private val repository: JobNoteRepository
) {
    suspend operator fun invoke(note: JobNote): JobNoteOutcome = repository.deleteNote(note)
}
