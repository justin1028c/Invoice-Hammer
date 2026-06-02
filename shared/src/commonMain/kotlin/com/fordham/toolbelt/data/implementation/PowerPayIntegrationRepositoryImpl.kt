package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.remote.PowerPayConfig
import com.fordham.toolbelt.data.remote.PowerPayEnvironment
import com.fordham.toolbelt.domain.model.PowerPayConnectionMode
import com.fordham.toolbelt.domain.repository.PowerPayIntegrationRepository

class PowerPayIntegrationRepositoryImpl(
    private val config: PowerPayConfig
) : PowerPayIntegrationRepository {

    override fun getConnectionMode(): PowerPayConnectionMode {
        if (!config.isConfigured) return PowerPayConnectionMode.Demo
        val envLabel = when (config.environment) {
            PowerPayEnvironment.Production -> "Production"
            PowerPayEnvironment.Sandbox -> "Sandbox"
        }
        return PowerPayConnectionMode.Live(
            environmentLabel = envLabel,
            presetLabel = config.preset.uppercase()
        )
    }
}
