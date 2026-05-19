package com.fordham.toolbelt.di

import com.fordham.toolbelt.worker.UnpaidInvoiceWorker
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.dsl.module

val workerModule = module {
    workerOf(::UnpaidInvoiceWorker)
}
