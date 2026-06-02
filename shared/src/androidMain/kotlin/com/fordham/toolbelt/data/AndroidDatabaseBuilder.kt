package com.fordham.toolbelt.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.fordham.toolbelt.util.SecurityManager
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

// Note: We'll inject this via Koin
class AndroidDatabaseBuilder(
    private val context: Context,
    private val securityManager: SecurityManager
) {
    fun create(): RoomDatabase.Builder<AppDatabase> {
        val dbFile = context.getDatabasePath("invoice_hammer.db")
        val passphrase = securityManager.getDatabasePassphrase().toByteArray()
        val factory = SupportOpenHelperFactory(passphrase)

        return Room.databaseBuilder<AppDatabase>(
            context = context,
            name = dbFile.absolutePath
        ).openHelperFactory(factory)
            .addMigrations(MIGRATION_19_20)
    }
}
