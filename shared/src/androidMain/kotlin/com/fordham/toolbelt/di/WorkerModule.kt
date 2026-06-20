package com.fordham.toolbelt.di

import com.fordham.toolbelt.worker.UnpaidInvoiceWorker
import com.fordham.toolbelt.worker.SyncQueueWorker
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.dsl.module

val workerModule = module {
    workerOf(::UnpaidInvoiceWorker)
    workerOf(::SyncQueueWorker)
}
