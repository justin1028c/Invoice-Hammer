package com.fordham.toolbelt.di

import com.fordham.toolbelt.data.*
import com.fordham.toolbelt.data.implementation.*
import com.fordham.toolbelt.data.remote.PowerPayClient
import com.fordham.toolbelt.data.remote.PowerPayConfig
import com.fordham.toolbelt.domain.repository.*
import com.fordham.toolbelt.domain.usecase.*
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module

fun initKoin(
    additionalModules: List<Module> = emptyList(),
    appDeclaration: KoinAppDeclaration = {}
) = startKoin {
    appDeclaration()
    modules(
        platformModule(),
        dataModule,
        useCaseModule,
        *additionalModules.toTypedArray()
    )
}

val dataModule = module {
    // Network
    single { HttpClient() }

    // Database & DAOs
    single { getRoomDatabase(get()) }
    single { get<AppDatabase>().clientDao() }
    single { get<AppDatabase>().invoiceDao() }
    single { get<AppDatabase>().receiptDao() }
    single { get<AppDatabase>().supplierDao() }
    single { get<AppDatabase>().photoDao() }
    single { get<AppDatabase>().jobNoteDao() }
    single { get<AppDatabase>().draftDao() }

    // Repositories
    single<ClientRepository> { RoomClientRepository(get()) }
    single<InvoiceRepository> { RoomInvoiceRepository(get()) }
    single<ReceiptRepository> { RoomReceiptRepository(get()) }
    single<JobNoteRepository> { RoomJobNoteRepository(get()) }
    single<SupplierRepository> { SupplierRepositoryImpl(get(), get()) }
    single<PhotoRepository> { RoomPhotoRepository(get()) }
    single<DraftRepository> { RoomDraftRepository(get()) }
    single<OcrRepository> { GeminiOcrRepository(get()) }
    single<GeminiRepository> { KtorGeminiRepository(get(), get(), get(), get(), get()) }
    single<AgentLlmGateway> { GeminiAgentLlmGateway(get(), get()) }
    single<ToolRegistry> { RepositoryToolRegistry(get(), get(), get(), get()) }
    single { PowerPayConfig(baseUrl = "") }
    single<PowerPayClient> {
        val config = get<PowerPayConfig>()
        if (config.isConfigured) {
            KtorPowerPayClient(get(), config)
        } else {
            MockPowerPayClient()
        }
    }
    single<PaymentRepository> { PowerPayPaymentRepository(get()) }
    single<ForemanAgentDispatchers> {
        object : ForemanAgentDispatchers {
            override val background = Dispatchers.Default
        }
    }
    single<SyncRepository> { KtorSyncRepository(get(), get(), get(), get(), get(), get(), get()) }
}

val useCaseModule = module {
    factory { AddSupplierUseCase(get()) }
    factory { AddJobNoteUseCase(get()) }
    factory { DeleteJobNoteUseCase(get()) }
    factory { DeleteClientUseCase(get()) }
    factory { LinkReceiptToClientUseCase(get()) }
    factory { SaveJobPhotoUseCase(get()) }
    factory { ProcessInvoiceAiUseCase(get()) }
    factory { BillLaborUseCase(get()) }
    factory { GenerateAndSaveInvoiceUseCase(get(), get(), get(), get(), get(), get(), get()) }
    factory { GenerateSummaryUseCase(get(), get()) }
    factory { GenerateTaxReportUseCase(get(), get(), get(), get()) }
    factory { CreatePaymentRequestUseCase(get()) }
    factory { GetPaymentLedgerUseCase(get()) }
    factory { GetBusinessStatsUseCase(get(), get()) }
    factory { GetClientFinancialSummaryUseCase(get(), get()) }
    factory { GetHiddenSuppliersUseCase(get()) }
    factory { GetSuppliersUseCase(get()) }
    factory { GlobalAiAgentUseCase(get(), get()) }
    factory { RunForemanAgentUseCase(get(), get(), get()) }
    factory { ForemanOrchestrator(get(), get(), get(), get()) }
    factory { HideSupplierUseCase(get()) }
    factory { LogSupplierPurchaseUseCase(get()) }
    factory { ProcessReceiptUseCase(get(), get(), get()) }
    factory { RestoreSupplierUseCase(get()) }
    factory { SaveInvoiceUseCase(get()) }
    factory { SeedSuppliersUseCase(get()) }
    factory { ToggleSupplierPinUseCase(get()) }
    factory { UpdateSupplierOrderUseCase(get()) }
}
