package com.fordham.toolbelt.domain.repository

interface LocalAiCapabilityRepository {
    suspend fun isOnDeviceAgentAvailable(): Boolean
}
