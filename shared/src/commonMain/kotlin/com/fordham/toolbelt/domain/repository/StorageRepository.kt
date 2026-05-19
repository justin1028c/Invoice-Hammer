package com.fordham.toolbelt.domain.repository

import com.fordham.toolbelt.domain.model.StorageOutcome
import com.fordham.toolbelt.domain.model.StorageBytesOutcome

interface StorageRepository {
    suspend fun saveBitmapBytesToPictures(imageBytes: ByteArray, prefix: String): StorageOutcome
    suspend fun getBytesFromUri(uriString: String): StorageBytesOutcome
    suspend fun saveUriToPictures(uriString: String, prefix: String): StorageOutcome
}
