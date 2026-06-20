package com.fordham.toolbelt.util

import kotlinx.coroutines.flow.Flow

expect class NetworkObserver {
    val isOnline: Flow<Boolean>
}
