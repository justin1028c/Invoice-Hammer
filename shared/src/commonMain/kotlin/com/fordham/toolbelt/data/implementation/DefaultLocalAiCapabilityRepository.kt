package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.local.LocalLlmEngine
import com.fordham.toolbelt.domain.repository.LocalAiCapabilityRepository

class DefaultLocalAiCapabilityRepository(
    private val localLlmEngine: LocalLlmEngine
) : LocalAiCapabilityRepository {
    override suspend fun isOnDeviceAgentAvailable(): Boolean = localLlmEngine.isSupported()
}
