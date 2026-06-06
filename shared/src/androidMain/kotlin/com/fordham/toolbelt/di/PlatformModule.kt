package com.fordham.toolbelt.di

import android.content.Context
import androidx.room.RoomDatabase
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
    single<VoiceAssistant> { AndroidVoiceAssistant(get()) }
    single { PlacesService() }
    single { SecurityManager(get()) }
    single<SecurityGateway> { get<SecurityManager>() }
    single { createDataStore { get<Context>().filesDir.resolve(DATASTORE_FILE_NAME).absolutePath } }
    single<SettingsRepository> { DataStoreSettingsRepository(get()) }
    single<com.fordham.toolbelt.domain.repository.StorageRepository> { AndroidStorageRepository(get(), get()) }
    single<DocumentExporter> { AndroidDocumentExporter(get(), get()) }
    single<com.fordham.toolbelt.domain.repository.InvoiceEngine> { AndroidPdfInvoiceEngine(get(), get()) }
    single { BentoReportEngine(get(), get()) }
    single<BentoReportGenerator> { AndroidBentoReportGenerator(get()) }
    single<com.fordham.toolbelt.util.TaxExporter> { AndroidTaxExporter(get(), get(), get()) }
    single<RoomDatabase.Builder<AppDatabase>> { AndroidDatabaseBuilder(get<Context>(), get()).create() }
    single<com.fordham.toolbelt.util.PlatformTarget> { currentPlatformTarget() }
    single<StoreBillingGateway> { AndroidStoreBillingGateway(get()) }
    single { AndroidStripePaymentSheetGateway() }
    single<StripePaymentSheetGateway> { get<AndroidStripePaymentSheetGateway>() }
    single<TapToPayGateway> { AndroidTapToPayGateway(get()) }
    single<BluetoothCardReaderGateway> { AndroidBluetoothCardReaderGateway() }
}
