package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.SyncOutcome
import com.fordham.toolbelt.domain.repository.SyncRepository

class RestoreSupabaseBackupUseCase(
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(): SyncOutcome = syncRepository.restoreFromSupabase()
}
