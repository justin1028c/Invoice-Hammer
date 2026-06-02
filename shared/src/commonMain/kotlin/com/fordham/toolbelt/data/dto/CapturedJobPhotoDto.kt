package com.fordham.toolbelt.data.dto

import com.fordham.toolbelt.domain.model.CapturedJobPhoto
import com.fordham.toolbelt.domain.model.JobPhotoPhase
import kotlinx.serialization.Serializable

@Serializable
data class CapturedJobPhotoDto(
    val uri: String,
    val phase: String
) {
    fun toDomain(): CapturedJobPhoto = CapturedJobPhoto(
        uri = uri,
        phase = when (phase.uppercase()) {
            "AFTER" -> JobPhotoPhase.After
            else -> JobPhotoPhase.Before
        }
    )

    companion object {
        fun fromDomain(photo: CapturedJobPhoto): CapturedJobPhotoDto = CapturedJobPhotoDto(
            uri = photo.uri,
            phase = when (photo.phase) {
                JobPhotoPhase.Before -> "BEFORE"
                JobPhotoPhase.After -> "AFTER"
            }
        )
    }
}
