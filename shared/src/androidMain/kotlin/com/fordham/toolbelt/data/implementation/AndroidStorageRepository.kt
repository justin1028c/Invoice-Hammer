package com.fordham.toolbelt.data.implementation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Environment
import com.fordham.toolbelt.domain.model.StorageOutcome
import com.fordham.toolbelt.domain.model.StorageBytesOutcome
import com.fordham.toolbelt.domain.repository.StorageRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class AndroidStorageRepository(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher
) : StorageRepository {

    override suspend fun saveBitmapBytesToPictures(imageBytes: ByteArray, prefix: String): StorageOutcome = withContext(ioDispatcher) {
        try {
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                ?: throw IllegalArgumentException("Failed to decode image bytes")
            
            val fileName = "${prefix}_${System.currentTimeMillis()}.jpg"
            val internalDir = File(context.filesDir, "vault/photos")
            if (!internalDir.exists()) internalDir.mkdirs()
            
            val file = File(internalDir, fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            
            StorageOutcome.Success(file.absolutePath)
        } catch (e: Exception) {
            StorageOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to save image bytes to internal storage"))
        }
    }

    override suspend fun getBytesFromUri(uriString: String): StorageBytesOutcome = withContext(ioDispatcher) {
        try {
            val uri = Uri.parse(uriString)
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it)
                } ?: throw IllegalArgumentException("Failed to open input stream for URI")
            }
            val bytes = bitmap.let { bmp ->
                ByteArrayOutputStream().use { stream ->
                    bmp.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                    stream.toByteArray()
                }
            }
            StorageBytesOutcome.Success(bytes)
        } catch (e: Exception) {
            StorageBytesOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to decode and retrieve image bytes from Uri"))
        }
    }

    override suspend fun saveUriToPictures(uriString: String, prefix: String): StorageOutcome = withContext(ioDispatcher) {
        try {
            val bytesResult = getBytesFromUri(uriString)
            if (bytesResult is StorageBytesOutcome.Success) {
                val saveResult = saveBitmapBytesToPictures(bytesResult.bytes, prefix)
                if (saveResult is StorageOutcome.Success) {
                    StorageOutcome.Success(saveResult.path)
                } else {
                    StorageOutcome.Failure((saveResult as StorageOutcome.Failure).error)
                }
            } else {
                StorageOutcome.Failure((bytesResult as StorageBytesOutcome.Failure).error)
            }
        } catch (e: Exception) {
            StorageOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to save Uri to internal storage"))
        }
    }
}
