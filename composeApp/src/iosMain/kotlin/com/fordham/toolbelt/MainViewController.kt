package com.fordham.toolbelt

import androidx.compose.ui.window.ComposeUIViewController
import com.fordham.toolbelt.data.implementation.IosAuthServiceProvider
import com.fordham.toolbelt.data.implementation.IosDriveAuthServiceProvider
import com.fordham.toolbelt.di.initKoin
import com.fordham.toolbelt.di.viewModelModule
import com.fordham.toolbelt.billing.IosStoreBillingServiceProvider
import com.fordham.toolbelt.stripe.IosStripePaymentBridgeProvider
import com.fordham.toolbelt.util.IosPlatformActionsServiceProvider
import com.fordham.toolbelt.util.IosSecurityServiceProvider
import platform.UIKit.UIViewController

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

fun MainViewController(): UIViewController = ComposeUIViewController {
    App()
}

fun initKoinIos() {
    checkIosBridgesInitialized()
    initKoin(
        additionalModules = listOf(viewModelModule)
    )
    CoroutineScope(Dispatchers.Default).launch {
        try {
            org.koin.core.context.GlobalContext.get().get<com.fordham.toolbelt.data.DatabaseProvider>().getDatabase()
        } catch (_: Exception) {}
    }
}

fun triggerIosBackgroundSync(completion: (Boolean) -> Unit) {
    val syncRepository = org.koin.core.context.GlobalContext.get().get<com.fordham.toolbelt.domain.repository.SyncRepository>()
    val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    scope.launch {
        when (val outcome = syncRepository.syncInvoices()) {
            is com.fordham.toolbelt.domain.model.SyncOutcome.Success -> completion(true)
            is com.fordham.toolbelt.domain.model.SyncOutcome.Failure -> completion(false)
        }
    }
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
    check(IosStoreBillingServiceProvider.bridge != null) {
        "iOS StoreKit bridge is missing. Register StoreBillingBridge() on IosStoreBillingServiceProvider.shared.bridge in iOSApp.swift before MainViewControllerKt.initKoinIos()."
    }
    check(IosStripePaymentBridgeProvider.bridge != null) {
        "iOS Stripe bridge is missing. Register StripePaymentBridge() on IosStripePaymentBridgeProvider.shared.bridge in iOSApp.swift before MainViewControllerKt.initKoinIos()."
    }
}

object DeepLinkRouter : org.koin.core.component.KoinComponent {
    private val dispatcher: com.fordham.toolbelt.domain.deeplink.DeepLinkDispatcher by inject()

    fun dispatch(url: String): Boolean {
        return dispatcher.dispatch(url)
    }
}

