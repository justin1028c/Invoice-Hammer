package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.remote.SupabaseConfig
import com.fordham.toolbelt.domain.model.SupabaseConnectionMode
import com.fordham.toolbelt.domain.repository.SupabaseIntegrationRepository

class SupabaseIntegrationRepositoryImpl(
    private val config: SupabaseConfig
) : SupabaseIntegrationRepository {

    override fun getConnectionMode(): SupabaseConnectionMode {
        if (!config.isConfigured) return SupabaseConnectionMode.Disabled
        val host = config.normalizedProjectUrl
            .removePrefix("https://")
            .removePrefix("http://")
        return SupabaseConnectionMode.Live(projectHost = host)
    }
}
