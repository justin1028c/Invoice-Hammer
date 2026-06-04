package com.fordham.toolbelt.di

import androidx.room.RoomDatabase
import com.fordham.toolbelt.data.AppDatabase
import com.fordham.toolbelt.data.implementation.IosAuthRepository
import com.fordham.toolbelt.data.implementation.IosDocumentExporter
import com.fordham.toolbelt.data.implementation.IosDriveAuthTokenProvider
import com.fordham.toolbelt.data.implementation.IosStorageRepository
import com.fordham.toolbelt.domain.repository.AuthRepository
import com.fordham.toolbelt.domain.repository.DocumentExporter
import com.fordham.toolbelt.domain.repository.DriveAuthTokenProvider
import com.fordham.toolbelt.domain.repository.StorageRepository
import com.fordham.toolbelt.util.IosPlatformActions
import com.fordham.toolbelt.util.PlatformActions
import com.fordham.toolbelt.util.SecretProvider
import com.fordham.toolbelt.data.*
import com.fordham.toolbelt.data.implementation.DataStoreSettingsRepository
import com.fordham.toolbelt.domain.repository.SettingsRepository
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import com.fordham.toolbelt.domain.repository.BentoReportGenerator
import com.fordham.toolbelt.pdf.IosBentoReportGenerator
import com.fordham.toolbelt.pdf.IosInvoiceEngine

import com.fordham.toolbelt.billing.IosStoreBillingGateway
import com.fordham.toolbelt.domain.repository.BluetoothCardReaderGateway
import com.fordham.toolbelt.domain.repository.StoreBillingGateway
import com.fordham.toolbelt.domain.repository.StripePaymentSheetGateway
import com.fordham.toolbelt.domain.repository.TapToPayGateway
import com.fordham.toolbelt.stripe.IosBluetoothCardReaderGateway
import com.fordham.toolbelt.stripe.IosStripePaymentSheetGateway
import com.fordham.toolbelt.stripe.IosTapToPayGateway
import com.fordham.toolbelt.util.IosSecurityServiceProvider
import com.fordham.toolbelt.util.currentPlatformTarget
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

actual fun platformModule(): Module = module {
    // Dispatcher — must be bound so KtorSyncRepository (and any other injected consumer) resolves correctly (ISSUE-06)
    single<CoroutineDispatcher> { Dispatchers.IO }
    single<SecretProvider> { com.fordham.toolbelt.util.IosSecretProvider() }
    single<DriveAuthTokenProvider> { IosDriveAuthTokenProvider() }
    single<PlatformActions> { IosPlatformActions() }
    single<PlatformGeofenceManager> { IosPlatformGeofenceManager() }
    single<PlatformNotificationManager> { IosPlatformNotificationManager() }
    single<KmpOcrEngine> { IosOcrEngine() }
    single<com.fordham.toolbelt.util.VoiceAssistant> { com.fordham.toolbelt.util.IosVoiceAssistant() }
    single { com.fordham.toolbelt.util.PlacesService() }
    single<AuthRepository> { IosAuthRepository(lazy { get<com.fordham.toolbelt.domain.repository.SubscriptionRepository>() }) }
    single { 
        createDataStore { 
            val documentDirectory = NSSearchPathForDirectoriesInDomains(
                NSDocumentDirectory, NSUserDomainMask, true
            ).first() as String
            "$documentDirectory/$DATASTORE_FILE_NAME"
        } 
    }
    single<SettingsRepository> { DataStoreSettingsRepository(get()) }
    single<StorageRepository> { IosStorageRepository() }
    single<DocumentExporter> { IosDocumentExporter() }
    single<com.fordham.toolbelt.domain.repository.InvoiceEngine> { IosInvoiceEngine() }
    single<BentoReportGenerator> { IosBentoReportGenerator() }
    single<com.fordham.toolbelt.util.TaxExporter> { com.fordham.toolbelt.util.IosTaxExporter(get(), get()) }
    single<RoomDatabase.Builder<AppDatabase>> {
        val passphrase = IosSecurityServiceProvider.bridge?.getDatabasePassphrase()
        check(passphrase != null) { "iOS Security Bridge/Keychain is NOT initialized" }

        getIosDatabaseBuilder(passphrase)
    }
    single<com.fordham.toolbelt.util.PlatformTarget> { currentPlatformTarget() }
    single<StoreBillingGateway> { IosStoreBillingGateway() }
    single { IosStripePaymentSheetGateway() }
    single<StripePaymentSheetGateway> { get<IosStripePaymentSheetGateway>() }
    single<TapToPayGateway> { IosTapToPayGateway(get()) }
    single<BluetoothCardReaderGateway> { IosBluetoothCardReaderGateway() }
}
