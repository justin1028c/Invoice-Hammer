package com.fordham.toolbelt.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {

    @Query("SELECT * FROM job_photos WHERE invoiceId = :invoiceId")
    fun getPhotosForInvoice(invoiceId: String): Flow<List<JobPhotoEntity>>

    @Query("SELECT * FROM job_photos WHERE invoiceId = :invoiceId")
    suspend fun getPhotosForInvoiceOnce(invoiceId: String): List<JobPhotoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: JobPhotoEntity)

    @Delete
    suspend fun deletePhoto(photo: JobPhotoEntity)
}
