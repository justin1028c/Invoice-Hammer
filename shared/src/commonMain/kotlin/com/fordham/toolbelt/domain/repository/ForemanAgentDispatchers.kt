package com.fordham.toolbelt.domain.repository

import kotlinx.coroutines.CoroutineDispatcher

interface ForemanAgentDispatchers {
    val background: CoroutineDispatcher
}
