package com.fordham.toolbelt.domain.repository

import com.fordham.toolbelt.domain.model.PowerPayEvent

interface PowerPayEventRepository {
    suspend fun pollClientEvents(): List<PowerPayEvent>
}
