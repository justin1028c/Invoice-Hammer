package com.fordham.toolbelt.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class SupabaseBackupRowDto(
    @SerialName("user_id") val userId: String,
    @SerialName("backup_json") val backupJson: JsonObject,
    @SerialName("exported_at_millis") val exportedAtMillis: Long,
    @SerialName("schema_version") val schemaVersion: Int = 1
)

data class SupabaseBackupUploadRequest(
    val userId: String,
    val backupJson: JsonObject,
    val exportedAtMillis: Long,
    val schemaVersion: Int = 1
)

@Serializable
data class SupabaseBackupRowResponseDto(
    @SerialName("user_id") val userId: String? = null,
    @SerialName("backup_json") val backupJson: JsonObject,
    @SerialName("exported_at_millis") val exportedAtMillis: Long? = null,
    @SerialName("schema_version") val schemaVersion: Int? = null
)
