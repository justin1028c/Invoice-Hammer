package com.fordham.toolbelt

import androidx.compose.ui.window.ComposeUIViewController
import com.fordham.toolbelt.data.implementation.IosAuthServiceProvider
import com.fordham.toolbelt.data.implementation.IosDriveAuthServiceProvider
import com.fordham.toolbelt.di.initKoin
import com.fordham.toolbelt.di.viewModelModule
import com.fordham.toolbelt.util.IosPlatformActionsServiceProvider
import com.fordham.toolbelt.util.IosSecurityServiceProvider
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController {
    App()
}

fun initKoinIos() {
    checkIosBridgesInitialized()
    initKoin(
        additionalModules = listOf(viewModelModule)
    )
}

private fun checkIosBridgesInitialized() {
    check(IosAuthServiceProvider.bridge != null) {
        "iOS auth bridge is missing. Register FirebaseAuthBridge() on IosAuthServiceProvider.shared.bridge in iOSApp.swift before MainViewControllerKt.initKoinIos()."
    }
    check(IosSecurityServiceProvider.bridge != null) {
        "iOS security/Keychain bridge is missing. Register KeyChainSecurityBridge() on IosSecurityServiceProvider.shared.bridge in iOSApp.swift before MainViewControllerKt.initKoinIos()."
    }
    check(IosPlatformActionsServiceProvider.bridge != null) {
        "iOS platform actions bridge is missing. Register PlatformActionsBridge() on IosPlatformActionsServiceProvider.shared.bridge in iOSApp.swift before MainViewControllerKt.initKoinIos()."
    }
    check(IosDriveAuthServiceProvider.bridge != null) {
        "iOS Drive auth bridge is missing. Register DriveAuthBridge() on IosDriveAuthServiceProvider.shared.bridge in iOSApp.swift before MainViewControllerKt.initKoinIos()."
    }
}
