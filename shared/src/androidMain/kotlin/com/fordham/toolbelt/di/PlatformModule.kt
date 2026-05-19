package com.fordham.toolbelt.di

import android.content.Context
import androidx.room.RoomDatabase
import com.fordham.toolbelt.data.AppDatabase
import com.fordham.toolbelt.data.SettingsDataStore
import com.fordham.toolbelt.pdf.BentoReportEngine
import com.fordham.toolbelt.pdf.InvoiceEngine
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
import com.fordham.toolbelt.util.VoiceAssistant
import com.fordham.toolbelt.util.*
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single { Dispatchers.IO }
    single<SecretProvider> { AndroidSecretProvider(get()) }
    single<DriveAuthTokenProvider> { AndroidDriveAuthTokenProvider(get()) }
    single<PlatformActions> { AndroidPlatformActions(get()) }
    single<AuthRepository> { FirebaseAuthRepository() }
    single<VoiceAssistant> { AndroidVoiceAssistant(get()) }
    single { PlacesService() }
    single { SecurityManager(get()) }
    single { createDataStore { get<Context>().filesDir.resolve(DATASTORE_FILE_NAME).absolutePath } }
    single<SettingsRepository> { DataStoreSettingsRepository(get()) }
    single<com.fordham.toolbelt.domain.repository.StorageRepository> { AndroidStorageRepository(get(), get()) }
    single<DocumentExporter> { AndroidDocumentExporter(get(), get()) }
    single<com.fordham.toolbelt.domain.repository.InvoiceEngine> { InvoiceEngine(get(), get()) }
    single { BentoReportEngine(get(), get()) }
    single<com.fordham.toolbelt.util.TaxExporter> { AndroidTaxExporter(get(), get(), get()) }
    single<RoomDatabase.Builder<AppDatabase>> { AndroidDatabaseBuilder(get<Context>(), get()).create() }
}
