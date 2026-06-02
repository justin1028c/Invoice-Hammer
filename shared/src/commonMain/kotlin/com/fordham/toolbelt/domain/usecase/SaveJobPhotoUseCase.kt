package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.repository.PhotoRepository
import com.fordham.toolbelt.util.randomUUID
import kotlinx.datetime.Clock

/**
 * Responsibility: Logic for creating and persisting a job photo record.
 */
class SaveJobPhotoUseCase(
    private val repository: PhotoRepository
) {
    suspend operator fun invoke(
        uriString: String,
        invoiceId: String,
        phase: JobPhotoPhase = JobPhotoPhase.Before
    ): PhotoOutcome {
        val photo = JobPhoto(
            id = PhotoId(randomUUID()),
            invoiceId = InvoiceId(invoiceId),
            localUri = uriString,
            phase = phase,
            timestamp = Clock.System.now().toEpochMilliseconds()
        )
        return repository.savePhoto(photo)
    }
}
