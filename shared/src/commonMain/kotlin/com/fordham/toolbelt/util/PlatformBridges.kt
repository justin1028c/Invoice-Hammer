package com.fordham.toolbelt.util

interface PlatformGeofenceManager {
    suspend fun registerClientGeofence(clientId: String, address: String)
    suspend fun removeClientGeofence(clientId: String)
}

interface PlatformNotificationManager {
    fun postBriefingNotification(title: String, body: String)
}

interface KmpOcrEngine {
    suspend fun extractText(imageBytes: ByteArray): String
}
