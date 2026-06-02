package com.fordham.toolbelt.domain.model

data class JobPhotoGalleryRow(
    val beforeUri: String?,
    val afterUri: String?
)

/**
 * Builds PDF gallery rows from capture order and explicit before/after tags.
 * - Each Before starts a new row (left column).
 * - Each After fills the right column on the most recent row that has before but no after.
 */
fun buildJobPhotoGalleryRows(photos: List<CapturedJobPhoto>): List<JobPhotoGalleryRow> {
    val rows = mutableListOf<JobPhotoGalleryRow>()
    for (photo in photos) {
        when (photo.phase) {
            JobPhotoPhase.Before -> {
                rows.add(JobPhotoGalleryRow(beforeUri = photo.uri, afterUri = null))
            }
            JobPhotoPhase.After -> {
                val openRowIndex = rows.indexOfLast { it.beforeUri != null && it.afterUri == null }
                if (openRowIndex >= 0) {
                    val open = rows[openRowIndex]
                    rows[openRowIndex] = open.copy(afterUri = photo.uri)
                } else {
                    rows.add(JobPhotoGalleryRow(beforeUri = null, afterUri = photo.uri))
                }
            }
        }
    }
    return rows
}

/** Flatten gallery rows back to a list for engines that draw one cell at a time. */
fun JobPhotoGalleryRow.toFlatCells(): List<Pair<JobPhotoPhase, String>> = buildList {
    beforeUri?.let { add(JobPhotoPhase.Before to it) }
    afterUri?.let { add(JobPhotoPhase.After to it) }
}

fun List<JobPhotoGalleryRow>.flattenForPdf(): List<Pair<JobPhotoPhase, String>> =
    flatMap { it.toFlatCells() }
