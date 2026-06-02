package com.fordham.toolbelt.data.remote

import com.fordham.toolbelt.domain.model.FailureMessage

interface SupabaseSubscriptionClient {
    suspend fun fetchActiveTiers(): SupabaseSubscriptionTiersOutcome
    suspend fun fetchUserEntitlement(userId: String): SupabaseUserEntitlementFetchOutcome
    suspend fun upsertUserEntitlement(request: SupabaseEntitlementUpsertRequest): SupabaseEntitlementUpsertOutcome
}

sealed interface SupabaseSubscriptionTiersOutcome {
    data class Success(val tiers: List<SupabaseSubscriptionTierDto>) : SupabaseSubscriptionTiersOutcome
    data object NotConfigured : SupabaseSubscriptionTiersOutcome
    data class Failure(val error: FailureMessage) : SupabaseSubscriptionTiersOutcome
}

sealed interface SupabaseUserEntitlementFetchOutcome {
    data class Success(val entitlement: SupabaseUserEntitlementDto) : SupabaseUserEntitlementFetchOutcome
    data object NotConfigured : SupabaseUserEntitlementFetchOutcome
    data object NotFound : SupabaseUserEntitlementFetchOutcome
    data class Failure(val error: FailureMessage) : SupabaseUserEntitlementFetchOutcome
}

sealed interface SupabaseEntitlementUpsertOutcome {
    data object Success : SupabaseEntitlementUpsertOutcome
    data object Skipped : SupabaseEntitlementUpsertOutcome
    data class Failure(val error: FailureMessage) : SupabaseEntitlementUpsertOutcome
}
