package com.fordham.toolbelt.data

import androidx.room.Room
import com.fordham.toolbelt.securevault.PlatformContext
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

internal actual fun buildRoomDatabase(
    passphrase: ByteArray,
    context: PlatformContext
): AppDatabase {
    val dbFile = context.context.getDatabasePath("invoice_hammer.db")
    val factory = SupportOpenHelperFactory(passphrase)

    return Room.databaseBuilder<AppDatabase>(
        context = context.context,
        name = dbFile.absolutePath
    ).openHelperFactory(factory)
        .fallbackToDestructiveMigration(dropAllTables = true)
        .addMigrations(MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22)
        .build()
}

internal actual fun generateSecurePassphrase(): String {
    val random = java.security.SecureRandom()
    val bytes = ByteArray(32)
    random.nextBytes(bytes)
    return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
}
