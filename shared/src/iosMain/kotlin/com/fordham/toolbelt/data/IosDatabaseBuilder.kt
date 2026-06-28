package com.fordham.toolbelt.data

import androidx.room.Room
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.fordham.toolbelt.securevault.PlatformContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSUUID

internal actual fun buildRoomDatabase(
    passphrase: ByteArray,
    context: PlatformContext
): AppDatabase {
    val dbFile = NSHomeDirectory() + "/Documents/invoice_hammer.db"
    val passphraseStr = passphrase.decodeToString()
    val escapedPassphrase = passphraseStr.replace("'", "''")
    return Room.databaseBuilder<AppDatabase>(
        name = dbFile,
        factory = { AppDatabaseConstructor.initialize() }
    ).setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .addMigrations(MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22)
        .addCallback(object : RoomDatabase.Callback() {
            override fun onOpen(db: SQLiteConnection) {
                db.prepare("PRAGMA key = '$escapedPassphrase'").step()
            }
        })
        .build()
}

internal actual fun generateSecurePassphrase(): String {
    return NSUUID().UUIDString()
}
