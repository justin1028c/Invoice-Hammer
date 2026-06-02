package com.fordham.toolbelt.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class SupabaseSubscriptionTierDto(
    val id: String,
    @SerialName("display_name") val displayName: String,
    val description: String = "",
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("google_play_product_id") val googlePlayProductId: String? = null,
    @SerialName("apple_product_id") val appleProductId: String? = null,
    @SerialName("billing_period") val billingPeriod: String = "monthly",
    @SerialName("price_label") val priceLabel: String = "",
    val features: JsonObject,
    @SerialName("is_active") val isActive: Boolean = true
)

@Serializable
data class SupabaseUserEntitlementDto(
    @SerialName("user_id") val userId: String,
    @SerialName("tier_id") val tierId: String,
    val source: String = "manual",
    @SerialName("purchase_token") val purchaseToken: String? = null,
    @SerialName("expires_at_millis") val expiresAtMillis: Long? = null,
    @SerialName("updated_at_millis") val updatedAtMillis: Long
)

data class SupabaseEntitlementUpsertRequest(
    val userId: String,
    val tierId: String,
    val source: String,
    val purchaseToken: String?,
    val expiresAtMillis: Long?,
    val updatedAtMillis: Long
)
