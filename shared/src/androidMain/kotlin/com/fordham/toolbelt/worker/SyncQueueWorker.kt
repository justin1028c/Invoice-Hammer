package com.fordham.toolbelt.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fordham.toolbelt.data.SyncQueueDao
import com.fordham.toolbelt.domain.model.SyncOutcome
import com.fordham.toolbelt.domain.repository.SyncRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.fordham.toolbelt.util.AppLogger

class SyncQueueWorker(
    context: Context,
    params: WorkerParameters,
    private val syncQueueDao: SyncQueueDao
) : CoroutineWorker(context, params), KoinComponent {

    private val syncRepository: SyncRepository by inject()

    override suspend fun doWork(): Result {
        val pending = syncQueueDao.getPendingOperations()
        if (pending.isEmpty()) {
            return Result.success()
        }

        AppLogger.d("SyncQueueWorker", "WorkManager task triggered. Processing ${pending.size} enqueued sync items...")

        return when (val outcome = syncRepository.syncInvoices()) {
            is SyncOutcome.Success -> {
                AppLogger.d("SyncQueueWorker", "Background sync backup succeeded. Dequeuing ${pending.size} items.")
                pending.forEach { syncQueueDao.dequeue(it) }
                Result.success()
            }
            is SyncOutcome.Failure -> {
                AppLogger.e("SyncQueueWorker", "Background sync backup failed: ${outcome.error.value}.")
                pending.forEach { 
                    if (it.retryCount >= 5) {
                        syncQueueDao.dequeue(it)
                    } else {
                        syncQueueDao.incrementRetry(it.id)
                    }
                }
                Result.retry()
            }
        }
    }
}
