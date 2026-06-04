package com.fordham.toolbelt.data.implementation

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.fordham.toolbelt.domain.model.DocumentCategory
import com.fordham.toolbelt.domain.model.DocumentExportOutcome
import com.fordham.toolbelt.domain.model.DocumentLocation
import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.repository.DocumentExporter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Publishes generated PDFs / ZIPs into the user-visible
 * `/storage/emulated/0/Documents/InvoiceHammer/<category>/` location so they show up
 * in the Files app alongside other downloads, while leaving the caller's source file
 * intact so the existing FileProvider-backed share flow keeps working unchanged.
 *
 * On Android 10+ (API 29) we go through MediaStore — required by scoped storage. On
 * older devices we fall back to a direct write under public Documents, and finally to
 * app-scoped `getExternalFilesDir(DIRECTORY_DOCUMENTS)` if the public copy is denied.
 */
class AndroidDocumentExporter(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher
) : DocumentExporter {

    override suspend fun publish(
        sourcePath: String,
        category: DocumentCategory,
        displayName: String
    ): DocumentExportOutcome = withContext(ioDispatcher) {
        try {
            val source = File(sourcePath)
            if (!source.exists() || !source.isFile) {
                return@withContext DocumentExportOutcome.Failure(
                    FailureMessage("Source file not found: $sourcePath")
                )
            }

            val mimeType = mimeTypeFor(displayName)
            val relativeFolder = "${Environment.DIRECTORY_DOCUMENTS}/$ROOT_FOLDER/${category.subdirectoryName}"
            val userVisiblePath = "$relativeFolder/$displayName"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val collection = MediaStore.Files.getContentUri("external")

                // Ensure relative folder ends with a slash for exact folder query match in MediaStore
                val queryFolder = if (relativeFolder.endsWith("/")) relativeFolder else "$relativeFolder/"
                val projection = arrayOf(MediaStore.MediaColumns._ID)
                val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} = ? AND ${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
                val selectionArgs = arrayOf(queryFolder, displayName)

                resolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                        val id = cursor.getLong(idColumn)
                        val existingUri = ContentUris.withAppendedId(collection, id)
                        try {
                            resolver.delete(existingUri, null, null)
                        } catch (e: Exception) {
                            com.fordham.toolbelt.util.AppLogger.e("AndroidDocumentExporter", "Failed to delete existing MediaStore entry: $existingUri", e)
                        }
                    }
                }

                val cv = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativeFolder)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val uri = resolver.insert(collection, cv)
                    ?: return@withContext DocumentExportOutcome.Failure(
                        FailureMessage("MediaStore refused to register $userVisiblePath")
                    )
                resolver.openOutputStream(uri)?.use { out ->
                    source.inputStream().use { it.copyTo(out) }
                } ?: return@withContext DocumentExportOutcome.Failure(
                    FailureMessage("MediaStore output stream unavailable for $userVisiblePath")
                )
                cv.clear()
                cv.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, cv, null, null)

                DocumentExportOutcome.Success(
                    DocumentLocation(shareablePath = source.absolutePath, userVisiblePath = userVisiblePath)
                )
            } else {
                val legacyRoot = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                val publicDir = File(legacyRoot, "$ROOT_FOLDER/${category.subdirectoryName}")
                val mirrored = copyToDirectoryOrFallback(source, publicDir, displayName, category)
                DocumentExportOutcome.Success(
                    DocumentLocation(shareablePath = source.absolutePath, userVisiblePath = mirrored)
                )
            }
        } catch (t: Throwable) {
            DocumentExportOutcome.Failure(
                FailureMessage(t.message ?: "Unknown failure publishing document")
            )
        }
    }

    /**
     * Best-effort write to public Documents on legacy Android. If that fails (e.g. no
     * WRITE_EXTERNAL_STORAGE permission) we mirror into the app-scoped Documents folder
     * so users still have a stable place under `Android/data/<pkg>/files/Documents/`.
     */
    private fun copyToDirectoryOrFallback(
        source: File,
        publicDir: File,
        displayName: String,
        category: DocumentCategory
    ): String {
        try {
            if (!publicDir.exists()) publicDir.mkdirs()
            val target = File(publicDir, displayName)
            source.inputStream().use { input ->
                target.outputStream().use { input.copyTo(it) }
            }
            return target.absolutePath
        } catch (_: Throwable) {
            val appScopedRoot = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                ?: File(context.filesDir, Environment.DIRECTORY_DOCUMENTS)
            val fallbackDir = File(appScopedRoot, "$ROOT_FOLDER/${category.subdirectoryName}")
            if (!fallbackDir.exists()) fallbackDir.mkdirs()
            val target = File(fallbackDir, displayName)
            source.inputStream().use { input ->
                target.outputStream().use { input.copyTo(it) }
            }
            return target.absolutePath
        }
    }

    private fun mimeTypeFor(displayName: String): String = when {
        displayName.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
        displayName.endsWith(".zip", ignoreCase = true) -> "application/zip"
        displayName.endsWith(".csv", ignoreCase = true) -> "text/csv"
        else -> "application/octet-stream"
    }

    companion object {
        const val ROOT_FOLDER = "InvoiceHammer"
    }
}
