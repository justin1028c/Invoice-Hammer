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
import com.fordham.toolbelt.data.*
import com.fordham.toolbelt.data.implementation.DataStoreSettingsRepository
import com.fordham.toolbelt.domain.repository.SettingsRepository
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import com.fordham.toolbelt.pdf.IosInvoiceEngine

import com.fordham.toolbelt.util.IosSecurityServiceProvider

actual fun platformModule(): Module = module {
    single<SecretProvider> { com.fordham.toolbelt.util.IosSecretProvider() }
    single<DriveAuthTokenProvider> { IosDriveAuthTokenProvider() }
    single<PlatformActions> { IosPlatformActions() }
    single<com.fordham.toolbelt.util.VoiceAssistant> { com.fordham.toolbelt.util.IosVoiceAssistant() }
    single { com.fordham.toolbelt.util.PlacesService() }
    single<AuthRepository> { IosAuthRepository() }
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
    single<com.fordham.toolbelt.util.TaxExporter> { com.fordham.toolbelt.util.IosTaxExporter() }
    single<RoomDatabase.Builder<AppDatabase>> {
        val passphrase = IosSecurityServiceProvider.bridge?.getDatabasePassphrase()
        check(passphrase != null) { "iOS Security Bridge/Keychain is NOT initialized" }

        getIosDatabaseBuilder(passphrase)
    }
}
