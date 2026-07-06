package com.fordham.toolbelt.di

import android.content.Context
import androidx.room.RoomDatabase
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import okhttp3.ConnectionSpec
import okhttp3.TlsVersion
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.http.URLProtocol
import com.fordham.toolbelt.data.AppDatabase
import com.fordham.toolbelt.domain.repository.BentoReportGenerator
import com.fordham.toolbelt.pdf.AndroidBentoReportGenerator
import com.fordham.toolbelt.pdf.BentoReportEngine
import com.fordham.toolbelt.pdf.AndroidPdfInvoiceEngine
import com.fordham.toolbelt.data.*
import com.fordham.toolbelt.data.implementation.DataStoreSettingsRepository
import com.fordham.toolbelt.domain.repository.SettingsRepository
import com.fordham.toolbelt.data.implementation.AndroidDocumentExporter
import com.fordham.toolbelt.data.implementation.AndroidStorageRepository
import com.fordham.toolbelt.data.implementation.AndroidDriveAuthTokenProvider
import com.fordham.toolbelt.data.implementation.FirebaseAuthRepository
import com.fordham.toolbelt.domain.repository.AuthRepository
import com.fordham.toolbelt.domain.repository.DocumentExporter
import com.fordham.toolbelt.domain.repository.DriveAuthTokenProvider
import com.fordham.toolbelt.billing.AndroidStoreBillingGateway
import com.fordham.toolbelt.domain.repository.BluetoothCardReaderGateway
import com.fordham.toolbelt.domain.repository.StoreBillingGateway
import com.fordham.toolbelt.domain.repository.StripePaymentSheetGateway
import com.fordham.toolbelt.domain.repository.TapToPayGateway
import com.fordham.toolbelt.stripe.AndroidBluetoothCardReaderGateway
import com.fordham.toolbelt.stripe.AndroidStripePaymentSheetGateway
import com.fordham.toolbelt.stripe.AndroidTapToPayGateway
import com.fordham.toolbelt.util.VoiceAssistant
import com.fordham.toolbelt.util.*
import com.fordham.toolbelt.util.currentPlatformTarget
import kotlinx.coroutines.Dispatchers
import com.fordham.toolbelt.securevault.SecureVaultGateway
import com.fordham.toolbelt.securevault.PlatformContext
import com.fordham.toolbelt.securevault.createSecureVault
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single { Dispatchers.IO }
    single<SecretProvider> { AndroidSecretProvider(get()) }
    singleOf(::AndroidPlatformActions) { bind<PlatformActions>() }
    singleOf(::AndroidPlatformGeofenceManager) { bind<PlatformGeofenceManager>() }
    singleOf(::AndroidPlatformNotificationManager) { bind<PlatformNotificationManager>() }
    single<KmpOcrEngine> { AndroidOcrEngine() }
    single<DriveAuthTokenProvider> {
        AndroidDriveAuthTokenProvider(get(), get<PlatformActions>() as AndroidPlatformActions)
    }
    single<AuthRepository> { FirebaseAuthRepository(get(), lazy { get<com.fordham.toolbelt.domain.repository.SubscriptionRepository>() }) }
    single<VoiceAssistant> { AndroidVoiceAssistant(get(), get()) }
    single { PlacesService() }
    single { SecurityManager(get()) }
    single<SecurityGateway> { get<SecurityManager>() }
    single { PlatformContext(get<Context>()) }
    single<SecureVaultGateway> { createSecureVault(get(), get()) }
    single { NetworkObserver() }
    single { createDataStore { get<Context>().filesDir.resolve(DATASTORE_FILE_NAME).absolutePath } }
    single<SettingsRepository> { DataStoreSettingsRepository(get()) }
    single<com.fordham.toolbelt.domain.repository.StorageRepository> { AndroidStorageRepository(get(), get()) }
    single<DocumentExporter> { AndroidDocumentExporter(get(), get()) }
    single<com.fordham.toolbelt.domain.repository.InvoiceEngine> { AndroidPdfInvoiceEngine(get(), get()) }
    single { BentoReportEngine(get(), get()) }
    single<BentoReportGenerator> { AndroidBentoReportGenerator(get()) }
    single<com.fordham.toolbelt.util.TaxExporter> { AndroidTaxExporter(get(), get(), get()) }
    single<com.fordham.toolbelt.util.PlatformTarget> { currentPlatformTarget() }
    single<StoreBillingGateway> { AndroidStoreBillingGateway(get()) }
    single { AndroidStripePaymentSheetGateway() }
    single<StripePaymentSheetGateway> { get<AndroidStripePaymentSheetGateway>() }
    single<TapToPayGateway> { AndroidTapToPayGateway(get()) }
    single<BluetoothCardReaderGateway> { AndroidBluetoothCardReaderGateway() }
    single<com.fordham.toolbelt.data.local.LocalLlmEngine> {
        com.fordham.toolbelt.data.local.AndroidLocalLlmEngine(
            context = get(),
            scope = get(),
            ioDispatcher = get(),
            secretProvider = get()
        )
    }
}

actual fun platformHttpClient(): HttpClient {
    val client = HttpClient(io.ktor.client.engine.okhttp.OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 30_000
            socketTimeoutMillis = 30_000
        }
        engine {
            config {
                val spec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                    .tlsVersions(TlsVersion.TLS_1_3)
                    .build()
                connectionSpecs(listOf(spec))
            }
        }
    }
    client.requestPipeline.intercept(HttpRequestPipeline.Before) {
        if (context.url.protocol == URLProtocol.HTTP) {
            context.url.protocol = URLProtocol.HTTPS
        }
    }
    return client
}
