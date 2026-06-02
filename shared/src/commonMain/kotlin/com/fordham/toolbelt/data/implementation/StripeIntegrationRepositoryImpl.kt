package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.remote.StripeConfig
import com.fordham.toolbelt.domain.repository.StripeIntegrationRepository

class StripeIntegrationRepositoryImpl(
    private val config: StripeConfig
) : StripeIntegrationRepository {
    override val isPaymentSheetReady: Boolean
        get() = config.isPaymentSheetReady
}
