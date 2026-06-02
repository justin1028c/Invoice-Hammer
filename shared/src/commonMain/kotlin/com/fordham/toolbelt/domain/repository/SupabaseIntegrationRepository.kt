package com.fordham.toolbelt.domain.repository

import com.fordham.toolbelt.domain.model.SupabaseConnectionMode

interface SupabaseIntegrationRepository {
    fun getConnectionMode(): SupabaseConnectionMode
}
