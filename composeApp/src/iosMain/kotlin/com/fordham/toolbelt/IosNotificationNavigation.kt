package com.fordham.toolbelt

import com.fordham.toolbelt.domain.model.agent.AppTab
import com.fordham.toolbelt.navigation.MainTabNavigation

fun handleIosNotificationNavigationTarget(target: String?) {
    val tab = AppTab.fromName(target ?: return) ?: return
    MainTabNavigation.request(tab)
}
