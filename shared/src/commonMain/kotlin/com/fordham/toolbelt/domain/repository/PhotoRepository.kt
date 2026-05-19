package com.fordham.toolbelt.domain.repository

import com.fordham.toolbelt.domain.model.JobPhoto
import com.fordham.toolbelt.domain.model.PhotoOutcome
import com.fordham.toolbelt.domain.model.PhotoListOutcome
import com.fordham.toolbelt.domain.model.InvoiceId
import kotlinx.coroutines.flow.Flow

interface PhotoRepository {
    fun observePhotosForInvoice(invoiceId: InvoiceId): Flow<List<JobPhoto>>
    suspend fun getPhotosForInvoiceOnce(invoiceId: InvoiceId): PhotoListOutcome
    suspend fun savePhoto(photo: JobPhoto): PhotoOutcome
    suspend fun deletePhoto(photo: JobPhoto): PhotoOutcome
}
