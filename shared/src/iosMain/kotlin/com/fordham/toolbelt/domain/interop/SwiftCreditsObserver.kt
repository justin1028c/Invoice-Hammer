package com.fordham.toolbelt.domain.interop

import com.fordham.toolbelt.domain.model.credits.HammerCreditsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Swift-safe architecture wrapper preventing generic pollution 
 * inside the native iOS application.
 */
class SwiftCreditsObserver(
    private val repository: HammerCreditsRepository
) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var job: Job? = null

    fun startObserving(onBalanceChanged: (Int) -> Unit) {
        job?.cancel()
        job = repository.balanceFlow
            .onEach { balance -> onBalanceChanged(balance.value) }
            .launchIn(scope)
    }

    fun stopObserving() {
        job?.cancel()
        job = null
    }
}
