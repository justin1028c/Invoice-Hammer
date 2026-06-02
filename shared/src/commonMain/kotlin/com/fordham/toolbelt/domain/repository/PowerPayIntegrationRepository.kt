package com.fordham.toolbelt.domain.repository

import com.fordham.toolbelt.domain.model.PowerPayConnectionMode

interface PowerPayIntegrationRepository {
    fun getConnectionMode(): PowerPayConnectionMode
}
