package com.fordham.toolbelt

import android.app.Application
import com.fordham.toolbelt.di.initKoin
import com.fordham.toolbelt.di.viewModelModule
import com.fordham.toolbelt.di.workerModule
import com.google.firebase.FirebaseApp
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ToolbeltApp : Application() {

    override fun onCreate() {
        super.onCreate()
        com.fordham.toolbelt.util.AndroidAppContext.application = this
        
        // Protocol: Initialize SQLCipher native libraries
        try {
            System.loadLibrary("sqlcipher")
        } catch (e: Exception) {
            android.util.Log.e("ToolbeltApp", "Failed to load sqlcipher", e)
        }
        
        FirebaseApp.initializeApp(this)

        // Initialize Koin
        initKoin(
            additionalModules = listOf(viewModelModule, workerModule)
        ) {
            androidLogger()
            androidContext(this@ToolbeltApp)
            workManagerFactory()
        }

        // Pre-resolve database asynchronously on a background thread
        CoroutineScope(Dispatchers.IO).launch {
            try {
                org.koin.core.context.GlobalContext.get().get<com.fordham.toolbelt.data.DatabaseProvider>().getDatabase()
            } catch (e: Exception) {
                android.util.Log.e("ToolbeltApp", "Failed to pre-resolve database", e)
            }
        }
    }
}
