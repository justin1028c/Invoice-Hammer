package com.fordham.toolbelt.domain.repository

import com.fordham.toolbelt.domain.model.DocumentCategory
import com.fordham.toolbelt.domain.model.DocumentExportOutcome

/**
 * Publishes an already-written local artifact under the platform's user-visible
 * "Documents/InvoiceHammer/<category>/" directory.
 *
 * Implementations own all platform-specific path resolution (Android MediaStore /
 * `Environment.DIRECTORY_DOCUMENTS`; iOS `NSDocumentDirectory`). Domain code must
 * never construct platform paths directly.
 *
 * `sourcePath` is consumed but never deleted: callers decide whether the working copy
 * should remain reachable for share/print flows.
 */
interface DocumentExporter {
    suspend fun publish(
        sourcePath: String,
        category: DocumentCategory,
        displayName: String
    ): DocumentExportOutcome
}
