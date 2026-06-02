package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.remote.SupabaseBackupDownloadOutcome
import com.fordham.toolbelt.data.remote.SupabaseBackupUploadOutcome
import com.fordham.toolbelt.data.remote.SupabaseBackupUploadRequest
import com.fordham.toolbelt.data.remote.SupabaseClient
import com.fordham.toolbelt.data.remote.SupabaseClientOutcome
import com.fordham.toolbelt.data.remote.SupabaseEntitlementUpsertOutcome
import com.fordham.toolbelt.data.remote.SupabaseEntitlementUpsertRequest
import com.fordham.toolbelt.data.remote.SupabaseSubscriptionClient
import com.fordham.toolbelt.data.remote.SupabaseSubscriptionTiersOutcome
import com.fordham.toolbelt.data.remote.SupabaseUserEntitlementFetchOutcome

class DisabledSupabaseClient : SupabaseClient, SupabaseSubscriptionClient {
    override suspend fun checkConnection(): SupabaseClientOutcome {
        return SupabaseClientOutcome.NotConfigured
    }

    override suspend fun uploadBackup(request: SupabaseBackupUploadRequest): SupabaseBackupUploadOutcome {
        return SupabaseBackupUploadOutcome.Skipped
    }

    override suspend fun downloadLatestBackup(userId: String): SupabaseBackupDownloadOutcome {
        return SupabaseBackupDownloadOutcome.NotConfigured
    }

    override suspend fun fetchActiveTiers(): SupabaseSubscriptionTiersOutcome =
        SupabaseSubscriptionTiersOutcome.NotConfigured

    override suspend fun fetchUserEntitlement(userId: String): SupabaseUserEntitlementFetchOutcome =
        SupabaseUserEntitlementFetchOutcome.NotConfigured

    override suspend fun upsertUserEntitlement(request: SupabaseEntitlementUpsertRequest): SupabaseEntitlementUpsertOutcome =
        SupabaseEntitlementUpsertOutcome.Skipped
}
