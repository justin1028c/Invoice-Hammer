package com.fordham.toolbelt.data

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection

val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(connection: SQLiteConnection) {
        connection.prepare(
            """
            CREATE TABLE IF NOT EXISTS payment_requests (
                id TEXT NOT NULL PRIMARY KEY,
                invoiceId TEXT NOT NULL,
                invoiceClientName TEXT NOT NULL,
                type TEXT NOT NULL,
                provider TEXT NOT NULL,
                requestedAmount REAL NOT NULL,
                status TEXT NOT NULL,
                paymentLink TEXT NOT NULL,
                createdAtMillis INTEGER NOT NULL,
                paidAtMillis INTEGER,
                stellarTransactionHash TEXT,
                stellarExplorerUrl TEXT,
                assetCode TEXT NOT NULL
            )
            """.trimIndent()
        ).use { statement ->
            statement.step()
        }
    }
}

val MIGRATION_20_21 = object : Migration(20, 21) {
    override fun migrate(connection: SQLiteConnection) {
        connection.prepare(
            """
            CREATE TABLE IF NOT EXISTS sync_queue (
                id TEXT NOT NULL PRIMARY KEY,
                operationType TEXT NOT NULL,
                createdAtMillis INTEGER NOT NULL,
                retryCount INTEGER NOT NULL
            )
            """.trimIndent()
        ).use { statement ->
            statement.step()
        }
    }
}

val MIGRATION_21_22 = object : Migration(21, 22) {
    override fun migrate(connection: SQLiteConnection) {
        connection.prepare(
            "ALTER TABLE receipt_items ADD COLUMN category TEXT NOT NULL DEFAULT 'Other'"
        ).use { statement ->
            statement.step()
        }
    }
}
