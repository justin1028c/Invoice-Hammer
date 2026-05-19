package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.domain.model.DocumentCategory
import com.fordham.toolbelt.domain.model.DocumentExportOutcome
import com.fordham.toolbelt.domain.model.DocumentLocation
import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.repository.DocumentExporter
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

/**
 * Publishes artifacts into the iOS app sandbox under
 * `<Documents>/InvoiceHammer/<category>/`. That's the directory surfaced through
 * the Files app when the app declares `UIFileSharingEnabled` /
 * `LSSupportsOpeningDocumentsInPlace`; either way, sharing via
 * `UIActivityViewController` works against the same sandbox path.
 *
 * The source file is moved (not copied) when it already lives in the iOS sandbox,
 * which keeps invoices, reports, and tax bundles at the canonical Documents
 * location for share/print flows without duplicating bytes on disk.
 */
class IosDocumentExporter : DocumentExporter {

    private val fileManager = NSFileManager.defaultManager

    override suspend fun publish(
        sourcePath: String,
        category: DocumentCategory,
        displayName: String
    ): DocumentExportOutcome {
        return try {
            if (!fileManager.fileExistsAtPath(sourcePath)) {
                return DocumentExportOutcome.Failure(
                    FailureMessage("Source file not found: $sourcePath")
                )
            }

            val documentsRoot = NSSearchPathForDirectoriesInDomains(
                NSDocumentDirectory,
                NSUserDomainMask,
                true
            ).firstOrNull() as? String
                ?: return DocumentExportOutcome.Failure(
                    FailureMessage("iOS Documents directory not resolved")
                )

            val targetDir = "$documentsRoot/$ROOT_FOLDER/${category.subdirectoryName}"
            if (!fileManager.fileExistsAtPath(targetDir)) {
                fileManager.createDirectoryAtPath(
                    targetDir,
                    withIntermediateDirectories = true,
                    attributes = null,
                    error = null
                )
            }

            val finalPath = "$targetDir/$displayName"

            if (sourcePath == finalPath) {
                return DocumentExportOutcome.Success(
                    DocumentLocation(shareablePath = finalPath, userVisiblePath = finalPath)
                )
            }

            if (fileManager.fileExistsAtPath(finalPath)) {
                fileManager.removeItemAtPath(finalPath, error = null)
            }

            val moved = fileManager.moveItemAtPath(sourcePath, finalPath, error = null)
            val deliveredPath = if (moved) finalPath else {
                val copied = fileManager.copyItemAtPath(sourcePath, finalPath, error = null)
                if (copied) finalPath else {
                    return DocumentExportOutcome.Failure(
                        FailureMessage("Could not place $displayName in Documents/$ROOT_FOLDER")
                    )
                }
            }

            DocumentExportOutcome.Success(
                DocumentLocation(shareablePath = deliveredPath, userVisiblePath = deliveredPath)
            )
        } catch (t: Throwable) {
            DocumentExportOutcome.Failure(
                FailureMessage(t.message ?: "Unknown failure publishing document on iOS")
            )
        }
    }

    companion object {
        const val ROOT_FOLDER = "InvoiceHammer"
    }
}
