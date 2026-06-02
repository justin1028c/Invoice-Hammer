package com.fordham.toolbelt.navigation

import com.fordham.toolbelt.domain.model.agent.AppTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Cross-platform pending main-tab navigation (e.g. notification tap → History).
 */
object MainTabNavigation {
    const val EXTRA_NAVIGATE_TO = "NAVIGATE_TO"
    const val TARGET_HISTORY = "HISTORY"

    private val _pendingTab = MutableStateFlow<AppTab?>(null)
    val pendingTab: StateFlow<AppTab?> = _pendingTab.asStateFlow()

    fun request(tab: AppTab) {
        _pendingTab.value = tab
    }

    fun requestFromLaunchExtra(raw: String?) {
        raw?.let { value -> AppTab.fromName(value)?.let { request(it) } }
    }

    fun clear() {
        _pendingTab.value = null
    }
}
