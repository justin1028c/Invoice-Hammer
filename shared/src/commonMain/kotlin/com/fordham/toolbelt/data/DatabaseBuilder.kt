package com.fordham.toolbelt.data

import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

/**
 * Room KMP query dispatcher — uses [kotlinx.coroutines.Dispatchers.IO] (not JVM-only java.util).
 * Safe on Android and iOS; injected dispatchers are preferred for repository I/O (see [KtorSyncRepository]).
 */
fun getRoomDatabase(
    builder: RoomDatabase.Builder<AppDatabase>,
    queryDispatcher: CoroutineDispatcher = Dispatchers.IO
): AppDatabase {
    return builder
        .setQueryCoroutineContext(queryDispatcher)
        .build()
}
