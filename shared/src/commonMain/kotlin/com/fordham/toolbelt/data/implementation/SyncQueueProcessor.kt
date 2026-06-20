package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.SyncQueueDao
import com.fordham.toolbelt.domain.model.SyncOutcome
import com.fordham.toolbelt.domain.repository.SyncRepository
import com.fordham.toolbelt.util.NetworkObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.fordham.toolbelt.util.AppLogger

class SyncQueueProcessor(
    private val syncQueueDao: SyncQueueDao,
    private val syncRepository: SyncRepository,
    private val networkObserver: NetworkObserver,
    private val coroutineScope: CoroutineScope
) {
    fun start() {
        coroutineScope.launch {
            networkObserver.isOnline.collectLatest { online ->
                if (online) {
                    syncQueueDao.getPendingCountFlow().collectLatest { count ->
                        if (count > 0) {
                            processPendingOperations()
                        }
                    }
                }
            }
        }
    }

    private suspend fun processPendingOperations() {
        val pending = syncQueueDao.getPendingOperations()
        if (pending.isEmpty()) return

        AppLogger.d("SyncQueueProcessor", "Processing ${pending.size} enqueued sync items...")

        when (val outcome = syncRepository.syncInvoices()) {
            is SyncOutcome.Success -> {
                AppLogger.d("SyncQueueProcessor", "Auto-sync backup succeeded. Dequeuing ${pending.size} items.")
                pending.forEach { syncQueueDao.dequeue(it) }
            }
            is SyncOutcome.Failure -> {
                AppLogger.e("SyncQueueProcessor", "Auto-sync backup failed: ${outcome.error.value}. Incrementing retries.")
                pending.forEach { 
                    if (it.retryCount >= 5) {
                        AppLogger.e("SyncQueueProcessor", "Sync item ${it.id} reached max retries. Dequeuing.")
                        syncQueueDao.dequeue(it)
                    } else {
                        syncQueueDao.incrementRetry(it.id)
                    }
                }
                delay(10000)
            }
        }
    }
}
