package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.domain.model.StorageOutcome
import com.fordham.toolbelt.domain.model.StorageBytesOutcome
import com.fordham.toolbelt.domain.repository.StorageRepository
import platform.Foundation.*
import platform.UIKit.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData = this.usePinned {
    NSData.create(bytes = it.addressOf(0), length = this.size.toULong())
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = this.length.toInt()
    val bytes = ByteArray(size)
    if (size > 0) {
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), this.bytes, this.length)
        }
    }
    return bytes
}

class IosStorageRepository : StorageRepository {
    private val fileManager = NSFileManager.defaultManager

    override suspend fun saveBitmapBytesToPictures(imageBytes: ByteArray, prefix: String): StorageOutcome = try {
        val fileName = "${prefix}_${NSDate().timeIntervalSince1970}.jpg"
        val docsDir = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true).first() as String
        val vaultDir = "$docsDir/vault/photos"
        
        if (!fileManager.fileExistsAtPath(vaultDir)) {
            fileManager.createDirectoryAtPath(vaultDir, withIntermediateDirectories = true, attributes = null, error = null)
        }
        
        val filePath = "$vaultDir/$fileName"
        val nsData = imageBytes.toNSData()
        nsData.writeToFile(filePath, atomically = true)
        
        StorageOutcome.Success(filePath)
    } catch (e: Exception) {
        StorageOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to save image bytes to internal iOS storage"))
    }

    override suspend fun getBytesFromUri(uriString: String): StorageBytesOutcome = try {
        val nsData = NSData.dataWithContentsOfFile(uriString) 
            ?: throw Exception("Could not read file at $uriString")
        StorageBytesOutcome.Success(nsData.toByteArray())
    } catch (e: Exception) {
        StorageBytesOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to decode and retrieve image bytes on iOS"))
    }

    override suspend fun saveUriToPictures(uriString: String, prefix: String): StorageOutcome = try {
        val fileName = "${prefix}_${NSDate().timeIntervalSince1970}.jpg"
        val docsDir = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true).first() as String
        val vaultDir = "$docsDir/vault/photos"
        
        if (!fileManager.fileExistsAtPath(vaultDir)) {
            fileManager.createDirectoryAtPath(vaultDir, withIntermediateDirectories = true, attributes = null, error = null)
        }
        
        val newFilePath = "$vaultDir/$fileName"
        fileManager.copyItemAtPath(uriString, newFilePath, error = null)
        
        StorageOutcome.Success(newFilePath)
    } catch (e: Exception) {
        StorageOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to save Uri path to internal iOS storage"))
    }
}
