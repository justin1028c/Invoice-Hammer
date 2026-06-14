package com.fordham.toolbelt.data

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import platform.Foundation.NSHomeDirectory

fun getIosDatabaseBuilder(passphrase: String): RoomDatabase.Builder<AppDatabase> {
    val dbFile = NSHomeDirectory() + "/Documents/invoice_hammer.db"
    // SQLite PRAGMA does not support parameter binding, so the passphrase must
    // be inlined. Double any single quotes to keep PRAGMA key syntax-safe even
    // if the Keychain bridge ever returns a non-UUID passphrase.
    val escapedPassphrase = passphrase.replace("'", "''")
    return Room.databaseBuilder<AppDatabase>(
        name = dbFile,
        factory = { AppDatabaseConstructor.initialize() }
    ).setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .addMigrations(MIGRATION_19_20)
        .addCallback(object : RoomDatabase.Callback() {
            override fun onOpen(db: SQLiteConnection) {
                db.prepare("PRAGMA key = '$escapedPassphrase'").step()
            }
        })
}
