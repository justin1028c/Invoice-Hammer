package com.fordham.toolbelt.di

import com.fordham.toolbelt.ui.viewmodel.*
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { SuppliersViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { ClientsViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { HistoryViewModel(get()) }
    viewModel { PaymentViewModel(get(), get()) }
    viewModel { StatsViewModel(get(), get(), get(), get()) }
    viewModel { AgentViewModel(get(), get()) }
    viewModel { ReceiptsViewModel(get(), get(), get(), get()) }
    viewModel { SharedViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { NewInvoiceViewModel(get(), get(), get(), get(), get()) }
    viewModel { AuthViewModel(get(), get()) }
}
