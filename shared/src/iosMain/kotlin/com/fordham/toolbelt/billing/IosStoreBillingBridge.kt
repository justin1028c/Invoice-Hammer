package com.fordham.toolbelt.billing

interface IosStoreBillingBridge {
    suspend fun purchase(productId: String): IosStoreBillingNativeResult
    suspend fun restorePurchases(): IosStoreBillingRestoreResult
    suspend fun finishTransaction(transactionId: String): Boolean
}

data class IosStoreBillingNativeResult(
    val success: Boolean,
    val cancelled: Boolean = false,
    val productId: String? = null,
    val transactionId: String? = null,
    val errorMessage: String? = null
)

data class IosStoreBillingRestoreResult(
    val success: Boolean,
    val activeProductIds: List<String> = emptyList(),
    val errorMessage: String? = null
)

object IosStoreBillingServiceProvider {
    var bridge: IosStoreBillingBridge? = null
}
