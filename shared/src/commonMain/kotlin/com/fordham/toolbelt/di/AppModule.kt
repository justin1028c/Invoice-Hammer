package com.fordham.toolbelt.di

import com.fordham.toolbelt.data.*
import com.fordham.toolbelt.data.implementation.*
import com.fordham.toolbelt.data.remote.PowerPayClient
import com.fordham.toolbelt.data.remote.PowerPayConfig
import com.fordham.toolbelt.data.remote.SupabaseClient
import com.fordham.toolbelt.data.remote.SupabaseConfig
import com.fordham.toolbelt.data.remote.SupabaseSubscriptionClient
import com.fordham.toolbelt.data.implementation.BetaCardTerminalPaymentGateway
import com.fordham.toolbelt.domain.repository.CardTerminalPaymentGateway
import com.fordham.toolbelt.domain.repository.StoreBillingGateway
import com.fordham.toolbelt.domain.repository.SubscriptionRepository
import com.fordham.toolbelt.util.PlatformTarget
import com.fordham.toolbelt.domain.repository.*
import com.fordham.toolbelt.domain.usecase.*
import com.fordham.toolbelt.domain.model.agent.ForemanSessionStore
import com.fordham.toolbelt.domain.repository.ForemanSessionPersistencePort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import com.fordham.toolbelt.domain.usecase.subscription.*
import com.fordham.toolbelt.domain.usecase.stripe.*
import com.fordham.toolbelt.data.remote.StripeConfig
import com.fordham.toolbelt.data.remote.StripePaymentBackendClient
import com.fordham.toolbelt.domain.deeplink.DeepLinkDispatcher
import com.fordham.toolbelt.domain.deeplink.DeepLinkDispatcherImpl
import com.fordham.toolbelt.domain.usecase.credits.EvaluateHammerCreditsAccessUseCase
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
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
    single { platformHttpClient() }

    // Database & DAOs
    single { getRoomDatabase(get()) }
    single { get<AppDatabase>().clientDao() }
    single { get<AppDatabase>().invoiceDao() }
    single { get<AppDatabase>().receiptDao() }
    single { get<AppDatabase>().supplierDao() }
    single { get<AppDatabase>().photoDao() }
    single { get<AppDatabase>().jobNoteDao() }
    single { get<AppDatabase>().draftDao() }
    single { get<AppDatabase>().paymentRequestDao() }

    // Repositories
    single<ClientRepository> { RoomClientRepository(get()) }
    single<InvoiceRepository> { RoomInvoiceRepository(get()) }
    single<ReceiptRepository> { RoomReceiptRepository(get()) }
    single<JobNoteRepository> { RoomJobNoteRepository(get()) }
    single<SupplierRepository> { SupplierRepositoryImpl(get(), get()) }
    single<PhotoRepository> { RoomPhotoRepository(get()) }
    single<DraftRepository> { RoomDraftRepository(get()) }
    single<OcrRepository> { GeminiOcrRepository(get()) }
    single { createDefaultForemanGeminiConfig(get()) }
    single<GeminiRepository> { KtorGeminiRepository(get(), get(), get(), get()) }
    single<ForemanJobMemoryPort> { RoomForemanJobMemoryAdapter(get()) }
    single { ForemanToolCallMapper(get(), get()) }
    single<AgentLlmGateway> {
        GeminiAgentLlmGateway(
            geminiRepository = get(),
            jobMemory = get(),
            toolCallMapper = get(),
            settingsRepository = get()
        )
    }
    single<ToolRegistry> {
        RepositoryToolRegistry(get(), get(), get(), get(), get(), get(), get(), get(), get(), get())
    }
    single { createDefaultPowerPayConfig(get()) }
    single { createDefaultSupabaseConfig(get()) }
    single { createDefaultStripeConfig(get()) }
    single<StripePaymentBackendClient> {
        val config = get<StripeConfig>()
        if (config.isBackendConfigured) {
            KtorStripePaymentBackendClient(get(), config)
        } else {
            DisabledStripePaymentBackendClient()
        }
    }
    single<StripeIntegrationRepository> { StripeIntegrationRepositoryImpl(get()) }
    single<StripePaymentIntentRepository> { StripePaymentIntentRepositoryImpl(get(), get()) }
    single<PowerPayIntegrationRepository> { PowerPayIntegrationRepositoryImpl(get()) }
    single<SupabaseIntegrationRepository> { SupabaseIntegrationRepositoryImpl(get()) }
    single<PowerPayClient> {
        val config = get<PowerPayConfig>()
        if (config.isConfigured) {
            KtorPowerPayClient(get(), config)
        } else {
            MockPowerPayClient()
        }
    }
    single<SupabaseClient> {
        val config = get<SupabaseConfig>()
        if (config.isConfigured) {
            KtorSupabaseClient(get(), config)
        } else {
            DisabledSupabaseClient()
        }
    }
    single<PaymentRepository> {
        RoutedPaymentRepository(get(), get(), get(), get(), get(), get())
    }
    single<DeepLinkDispatcher> { DeepLinkDispatcherImpl() }
    single<StripeCheckoutRepository> { StripeCheckoutRepositoryImpl(get(), get()) }
    single<PowerPayEventRepository> { PowerPayEventRepositoryImpl(get(), get()) }
    single<ForemanAgentDispatchers> {
        object : ForemanAgentDispatchers {
            override val background = Dispatchers.Default
        }
    }
    single<SyncRepository> {
        KtorSyncRepository(
            httpClient = get(),
            driveAuthTokenProvider = get(),
            supabaseClient = get(),
            supabaseConfig = get(),
            authRepository = get(),
            invoiceRepository = get(),
            receiptRepository = get(),
            clientRepository = get(),
            supplierRepository = get(),
            settingsRepository = get(),
            ioDispatcher = get()
        )
    }
    single<SupabaseSubscriptionClient> {
        get<SupabaseClient>() as SupabaseSubscriptionClient
    }
    single<SubscriptionRepository> {
        SubscriptionRepositoryImpl(
            supabaseSubscriptionClient = get(),
            storeBillingGateway = get(),
            authRepository = get(),
            settingsRepository = get(),
            platformTarget = get()
        )
    }
    single<BillingRepository> {
        BillingRepositoryImpl(
            supabaseSubscriptionClient = get(),
            storeBillingGateway = get(),
            authRepository = get(),
            settingsRepository = get(),
            platformTarget = get(),
            httpClient = get(),
            supabaseConfig = get()
        )
    }
    single<CardTerminalPaymentGateway> { BetaCardTerminalPaymentGateway() }
}

val useCaseModule = module {
    factory { AddSupplierUseCase(get()) }
    factory { AddJobNoteUseCase(get()) }
    factory { DeleteJobNoteUseCase(get()) }
    factory { DeleteClientUseCase(get()) }
    factory { LinkReceiptToClientUseCase(get()) }
    factory { SaveJobPhotoUseCase(get()) }
    factory { SaveBusinessLogoUseCase(get(), get()) }
    factory { ProcessInvoiceAiUseCase(get(), get()) }
    factory { BillLaborUseCase(get()) }
    factory { GenerateAndSaveInvoiceUseCase(get(), get(), get(), get(), get(), get(), get()) }
    factory { GenerateSummaryUseCase(get(), get()) }
    factory { GenerateTaxReportUseCase(get(), get(), get(), get()) }
    factory { HasSubscriptionFeatureUseCase(get()) }
    factory { RefreshSubscriptionCatalogUseCase(get()) }
    factory { SyncSubscriptionEntitlementUseCase(get()) }
    factory { ObserveUserEntitlementUseCase(get()) }
    factory { GetPaywallTiersUseCase(get()) }
    factory { PurchaseSubscriptionTierUseCase(get()) }
    factory { RestoreSubscriptionPurchasesUseCase(get()) }
    factory { EvaluateFeatureAccessUseCase(get()) }
    factory { EvaluateHammerCreditsAccessUseCase(get(), get()) }
    factory { ConsumeTokenUseCase(get()) }
    factory { PurchaseProductUseCase(get()) }
    factory { ReconcileTokensUseCase(get()) }
    factory { ObserveTokenBalancesUseCase(get()) }
    factory { CreatePaymentRequestUseCase(get()) }
    factory { GetPaymentLedgerUseCase(get()) }
    factory { HandlePowerPayEventUseCase(get(), get()) }
    factory { PollPowerPayClientEventsUseCase(get(), get()) }
    factory { GetPowerPayConnectionModeUseCase(get()) }
    factory { GetSupabaseConnectionModeUseCase(get()) }
    factory { GetBusinessStatsUseCase(get(), get()) }
    factory { GetClientFinancialSummaryUseCase(get(), get()) }
    factory { GetHiddenSuppliersUseCase(get()) }
    factory { GetSuppliersUseCase(get()) }
    factory {
        RunForemanAgentUseCase(
            llmGateway = get(),
            toolRegistry = get(),
            draftRepository = get(),
            dispatchers = get(),
            hasSubscriptionFeature = get(),
            consumeToken = get(),
            platformActions = get(),
            settingsRepository = get()
        )
    }
    single { CoroutineScope(SupervisorJob() + get<kotlinx.coroutines.CoroutineDispatcher>()) }
    single<ForemanSessionPersistencePort> { DataStoreForemanSessionPersistence(get()) }
    single { ForemanSessionStore(get(), get()) }
    factory { ForemanOrchestrator(get(), get()) }
    factory { HideSupplierUseCase(get()) }
    factory { LogSupplierPurchaseUseCase(get()) }
    factory { ProcessReceiptUseCase(get(), get(), get()) }
    factory { ProcessCardTerminalPaymentUseCase(get(), get(), get()) }
    factory { GetStripePaymentModeUseCase(get()) }
    factory { RefreshStripeConnectStatusUseCase(get(), get(), get()) }
    factory { StartStripeConnectOnboardingUseCase(get(), get(), get()) }
    factory { ProcessStripePaymentSheetUseCase(get(), get(), get(), get(), get(), get()) }
    factory { ProcessTapToPayUseCase(get(), get(), get(), get(), get(), get()) }
    factory { ProcessBluetoothReaderPaymentUseCase(get(), get(), get(), get(), get(), get(), get()) }
    factory { RestoreSupplierUseCase(get()) }
    factory { SaveInvoiceUseCase(get()) }
    factory { SeedSuppliersUseCase(get()) }
    factory { ToggleSupplierPinUseCase(get()) }
    factory { UpdateSupplierOrderUseCase(get()) }
    factory { SyncUnpaidInvoiceRemindersUseCase(get(), get(), get()) }
    factory { RestoreSupabaseBackupUseCase(get()) }
}
