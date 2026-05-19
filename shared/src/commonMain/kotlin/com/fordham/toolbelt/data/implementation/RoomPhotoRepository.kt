package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.PhotoDao
import com.fordham.toolbelt.data.toDomain
import com.fordham.toolbelt.data.toEntity
import com.fordham.toolbelt.domain.model.JobPhoto
import com.fordham.toolbelt.domain.model.PhotoOutcome
import com.fordham.toolbelt.domain.model.PhotoListOutcome
import com.fordham.toolbelt.domain.model.InvoiceId
import com.fordham.toolbelt.domain.repository.PhotoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomPhotoRepository(
    private val photoDao: PhotoDao
) : PhotoRepository {
    override fun observePhotosForInvoice(invoiceId: InvoiceId): Flow<List<JobPhoto>> =
        photoDao.getPhotosForInvoice(invoiceId.value).map { list -> list.map { it.toDomain() } }

    override suspend fun getPhotosForInvoiceOnce(invoiceId: InvoiceId): PhotoListOutcome = try {
        val photos = photoDao.getPhotosForInvoiceOnce(invoiceId.value).map { it.toDomain() }
        PhotoListOutcome.Success(photos)
    } catch (e: Exception) {
        PhotoListOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to retrieve photos once"))
    }

    override suspend fun savePhoto(photo: JobPhoto): PhotoOutcome = try {
        photoDao.insertPhoto(photo.toEntity())
        PhotoOutcome.Success
    } catch (e: Exception) {
        PhotoOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to save photo"))
    }

    override suspend fun deletePhoto(photo: JobPhoto): PhotoOutcome = try {
        photoDao.deletePhoto(photo.toEntity())
        PhotoOutcome.Success
    } catch (e: Exception) {
        PhotoOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to delete photo"))
    }
}
