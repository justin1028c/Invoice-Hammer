package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.DatabaseProvider
import com.fordham.toolbelt.domain.model.SyncOutcome
import com.fordham.toolbelt.domain.repository.SyncRepository
import com.fordham.toolbelt.util.NetworkObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.fordham.toolbelt.util.AppLogger

public class SyncQueueProcessor(
    private val databaseProvider: DatabaseProvider,
    private val syncRepository: SyncRepository,
    private val networkObserver: NetworkObserver,
    private val coroutineScope: CoroutineScope
) {
    private suspend fun syncQueueDao() = databaseProvider.getDatabase().syncQueueDao()

    public fun start() {
        coroutineScope.launch {
            networkObserver.isOnline.collectLatest { online ->
                if (online) {
                    val dao = syncQueueDao()
                    dao.getPendingCountFlow().collectLatest { count ->
                        if (count > 0) {
                            processPendingOperations()
                        }
                    }
                }
            }
        }
    }

    private suspend fun processPendingOperations() {
        val dao = syncQueueDao()
        val pending = dao.getPendingOperations()
        if (pending.isEmpty()) return

        AppLogger.d("SyncQueueProcessor", "Processing ${pending.size} enqueued sync items...")

        when (val outcome = syncRepository.syncInvoices()) {
            is SyncOutcome.Success -> {
                AppLogger.d("SyncQueueProcessor", "Auto-sync backup succeeded. Dequeuing ${pending.size} items.")
                pending.forEach { dao.dequeue(it) }
            }
            is SyncOutcome.Failure -> {
                AppLogger.e("SyncQueueProcessor", "Auto-sync backup failed: ${outcome.error.value}. Incrementing retries.")
                pending.forEach { 
                    if (it.retryCount >= 5) {
                        AppLogger.e("SyncQueueProcessor", "Sync item ${it.id} reached max retries. Dequeuing.")
                        dao.dequeue(it)
                    } else {
                        dao.incrementRetry(it.id)
                    }
                }
                delay(10000)
            }
        }
    }
}
