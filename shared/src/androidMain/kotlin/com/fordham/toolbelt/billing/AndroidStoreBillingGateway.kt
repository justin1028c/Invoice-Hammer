package com.fordham.toolbelt.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.model.subscription.ProductId
import com.fordham.toolbelt.domain.model.subscription.PurchaseToken
import com.fordham.toolbelt.domain.model.subscription.SubscriptionTier
import com.fordham.toolbelt.domain.repository.StoreBillingGateway
import com.fordham.toolbelt.domain.repository.StoreProductQueryOutcome
import com.fordham.toolbelt.domain.repository.StorePurchaseOutcome
import com.fordham.toolbelt.domain.repository.StorePurchaseResult
import com.fordham.toolbelt.domain.repository.StoreRestoreOutcome
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AndroidStoreBillingGateway(
    private val context: Context
) : StoreBillingGateway, PurchasesUpdatedListener {

    private var billingClient: BillingClient? = null
    private var productDetailsById: Map<String, com.android.billingclient.api.ProductDetails> = emptyMap()
    private var pendingPurchase: CompletableDeferred<StorePurchaseOutcome>? = null

    private val consumableProductIds = setOf(
        "scan_token_pack_15",
        "scan_token_pack_40",
        "invoice_pack_10",
        "tax_export_oneshot",
        "hammer-credit-pack-50",
        "hammer-credit-pack-150",
        "hammer-credit-pack-400"
    )

    override suspend fun queryProductIds(tiers: List<SubscriptionTier>): StoreProductQueryOutcome {
        val subscriptionIds = tiers.mapNotNull { it.googlePlayProductId }.distinct()
        val consumableIds = consumableProductIds.toList()
        val allIds = (subscriptionIds + consumableIds).distinct()

        connect()
        val client = billingClient ?: return StoreProductQueryOutcome.Failure(
            FailureMessage("Google Play Billing is not available.")
        )

        return suspendCancellableCoroutine { cont ->
            val detailsMap = mutableMapOf<String, com.android.billingclient.api.ProductDetails>()
            var queriesCompleted = 0
            var queryError: FailureMessage? = null

            fun checkCompletion() {
                queriesCompleted++
                if (queriesCompleted == 2) {
                    if (detailsMap.isNotEmpty()) {
                        productDetailsById = productDetailsById + detailsMap
                        cont.resume(StoreProductQueryOutcome.Success(allIds))
                    } else {
                        cont.resume(StoreProductQueryOutcome.Failure(queryError ?: FailureMessage("No products could be loaded.")))
                    }
                }
            }

            // Subscriptions
            if (subscriptionIds.isNotEmpty()) {
                val subProducts = subscriptionIds.map { id ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(id)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                }
                val subParams = QueryProductDetailsParams.newBuilder().setProductList(subProducts).build()
                client.queryProductDetailsAsync(subParams) { billingResult, detailsList ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && detailsList != null) {
                        detailsList.forEach { detailsMap[it.productId] = it }
                    } else {
                        queryError = FailureMessage(billingResult.debugMessage ?: "Subscription query failed.")
                    }
                    checkCompletion()
                }
            } else {
                checkCompletion()
            }

            // Consumables
            if (consumableIds.isNotEmpty()) {
                val inappProducts = consumableIds.map { id ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(id)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                }
                val inappParams = QueryProductDetailsParams.newBuilder().setProductList(inappProducts).build()
                client.queryProductDetailsAsync(inappParams) { billingResult, detailsList ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && detailsList != null) {
                        detailsList.forEach { detailsMap[it.productId] = it }
                    } else {
                        if (queryError == null) {
                            queryError = FailureMessage(billingResult.debugMessage ?: "Consumable query failed.")
                        }
                    }
                    checkCompletion()
                }
            } else {
                checkCompletion()
            }
        }
    }

    override suspend fun purchase(productId: String): StorePurchaseOutcome {
        connect()
        val client = billingClient ?: return StorePurchaseOutcome.Failure(
            FailureMessage("Google Play Billing is not available.")
        )
        val activity = BillingActivityHolder.activity
            ?: return StorePurchaseOutcome.Failure(
                FailureMessage("Billing requires an active screen. Reopen the app and try again.")
            )
        val details = productDetailsById[productId]
            ?: return StorePurchaseOutcome.Failure(
                FailureMessage("Product $productId was not loaded from Play Console.")
            )

        val deferred = CompletableDeferred<StorePurchaseOutcome>()
        pendingPurchase = deferred

        val productDetailsParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)

        if (details.productType == BillingClient.ProductType.SUBS) {
            val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
                ?: return StorePurchaseOutcome.Failure(
                    FailureMessage("No subscription offer token for $productId.")
                )
            productDetailsParamsBuilder.setOfferToken(offerToken)
        }

        val productDetailsParams = productDetailsParamsBuilder.build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        val launchResult = client.launchBillingFlow(activity, flowParams)
        if (launchResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            pendingPurchase = null
            return StorePurchaseOutcome.Cancelled
        }
        if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
            pendingPurchase = null
            return StorePurchaseOutcome.Failure(
                FailureMessage(launchResult.debugMessage ?: "Could not start Google Play purchase.")
            )
        }

        return deferred.await()
    }

    override suspend fun restorePurchases(): StoreRestoreOutcome {
        connect()
        val client = billingClient ?: return StoreRestoreOutcome.Failure(
            FailureMessage("Google Play Billing is not available.")
        )
        return suspendCancellableCoroutine { cont ->
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
            client.queryPurchasesAsync(params) { billingResult, purchases ->
                if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    cont.resume(
                        StoreRestoreOutcome.Failure(
                            FailureMessage(billingResult.debugMessage ?: "Restore failed.")
                        )
                    )
                    return@queryPurchasesAsync
                }
                val active = purchases
                    .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                    .flatMap { it.products }
                cont.resume(StoreRestoreOutcome.Success(active))
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        val deferred = pendingPurchase ?: return
        pendingPurchase = null
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val purchase = purchases?.firstOrNull()
                if (purchase == null) {
                    deferred.complete(StorePurchaseOutcome.Failure(FailureMessage("Purchase completed without receipt.")))
                } else {
                    acknowledgeOrConsumeIfNeeded(purchase)
                    val productId = purchase.products.firstOrNull().orEmpty()
                    deferred.complete(
                        StorePurchaseOutcome.Success(
                            StorePurchaseResult(
                                productId = productId,
                                purchaseToken = purchase.purchaseToken,
                                isAcknowledged = purchase.isAcknowledged
                            )
                        )
                    )
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED ->
                deferred.complete(StorePurchaseOutcome.Cancelled)
            else ->
                deferred.complete(
                    StorePurchaseOutcome.Failure(
                        FailureMessage(billingResult.debugMessage ?: "Purchase failed.")
                    )
                )
        }
    }

    private fun acknowledgeOrConsumeIfNeeded(purchase: Purchase) {
        val client = billingClient ?: return
        val productId = purchase.products.firstOrNull() ?: return
        if (!consumableProductIds.contains(productId)) {
            if (purchase.isAcknowledged) return
            val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            client.acknowledgePurchase(acknowledgeParams) { }
        }
    }

    override suspend fun finalizeConsumable(productId: ProductId, token: PurchaseToken): Boolean {
        if (!consumableProductIds.contains(productId.value)) return false
        connect()
        val client = billingClient ?: return false
        return suspendCancellableCoroutine { cont ->
            val consumeParams = com.android.billingclient.api.ConsumeParams.newBuilder()
                .setPurchaseToken(token.value)
                .build()
            client.consumeAsync(consumeParams) { billingResult, _ ->
                cont.resume(billingResult.responseCode == BillingClient.BillingResponseCode.OK)
            }
        }
    }

    private suspend fun connect() {
        if (billingClient?.isReady == true) return
        suspendCancellableCoroutine { cont ->
            val client = BillingClient.newBuilder(context.applicationContext)
                .setListener(this)
                .enablePendingPurchases()
                .build()
            billingClient = client
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    cont.resume(Unit)
                }

                override fun onBillingServiceDisconnected() {
                    billingClient = null
                }
            })
        }
    }
}
