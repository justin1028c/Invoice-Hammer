package com.fordham.toolbelt.di

import com.fordham.toolbelt.ui.viewmodel.*
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { SuppliersViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { ClientsViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { HistoryViewModel(get(), get()) }
    viewModel { InvoicePaymentViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { StripeConnectViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { StripeTerminalViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel {
        val invoicePaymentVm = get<InvoicePaymentViewModel>()
        val connectVm = StripeConnectViewModel(
            invoicePaymentViewModel = invoicePaymentVm,
            createPaymentRequestUseCase = get(),
            getStripePaymentModeUseCase = get(),
            stripeCheckoutRepository = get(),
            resolveStripeCheckoutLinkUseCase = get(),
            platformActions = get(),
            authRepository = get(),
            invoiceRepository = get()
        )
        val terminalVm = StripeTerminalViewModel(
            invoicePaymentViewModel = invoicePaymentVm,
            processCardTerminalPaymentUseCase = get(),
            cardTerminalGateway = get(),
            processStripePaymentSheetUseCase = get(),
            processTapToPayUseCase = get(),
            processBluetoothReaderPaymentUseCase = get()
        )
        PaymentViewModel(invoicePaymentVm, connectVm, terminalVm)
    }
    viewModel { StatsViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { AgentViewModel(get()) }
    viewModel { ReceiptsViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { SharedViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { NewInvoiceViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { AuthViewModel(get(), get(), get(), get(), get()) }
    viewModel { SubscriptionViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get(), get(), get(), get()) }
    viewModel { JobsiteIntelligenceViewModel(get()) }
}
