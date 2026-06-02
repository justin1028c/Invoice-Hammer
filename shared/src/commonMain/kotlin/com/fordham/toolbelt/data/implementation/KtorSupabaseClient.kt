package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.remote.SupabaseBackupDownloadOutcome
import com.fordham.toolbelt.data.remote.SupabaseBackupRowDto
import com.fordham.toolbelt.data.remote.SupabaseBackupRowResponseDto
import com.fordham.toolbelt.data.remote.SupabaseBackupUploadOutcome
import com.fordham.toolbelt.data.remote.SupabaseBackupUploadRequest
import com.fordham.toolbelt.data.remote.SupabaseClient
import com.fordham.toolbelt.data.remote.SupabaseClientOutcome
import com.fordham.toolbelt.data.remote.SupabaseConfig
import com.fordham.toolbelt.data.remote.SupabaseEntitlementUpsertOutcome
import com.fordham.toolbelt.data.remote.SupabaseEntitlementUpsertRequest
import com.fordham.toolbelt.data.remote.SupabaseSubscriptionClient
import com.fordham.toolbelt.data.remote.SupabaseSubscriptionTierDto
import com.fordham.toolbelt.data.remote.SupabaseSubscriptionTiersOutcome
import com.fordham.toolbelt.data.remote.SupabaseUserEntitlementDto
import com.fordham.toolbelt.data.remote.SupabaseUserEntitlementFetchOutcome
import com.fordham.toolbelt.domain.model.FailureMessage
import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class KtorSupabaseClient(
    private val httpClient: HttpClient,
    private val config: SupabaseConfig
) : SupabaseClient, SupabaseSubscriptionClient {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun checkConnection(): SupabaseClientOutcome {
        if (!config.isConfigured) {
            return SupabaseClientOutcome.NotConfigured
        }

        return try {
            val response = httpClient.get("${config.normalizedProjectUrl}/rest/v1/") {
                applySupabaseHeaders()
            }
            if (response.status.isSuccess()) {
                SupabaseClientOutcome.Connected
            } else {
                SupabaseClientOutcome.Failure(
                    FailureMessage("Supabase connection failed with HTTP ${response.status.value}. ${response.bodyAsText()}")
                )
            }
        } catch (e: Exception) {
            SupabaseClientOutcome.Failure(FailureMessage(e.message ?: "Supabase connection failed."))
        }
    }

    override suspend fun uploadBackup(request: SupabaseBackupUploadRequest): SupabaseBackupUploadOutcome {
        if (!config.isConfigured) {
            return SupabaseBackupUploadOutcome.Skipped
        }

        val row = SupabaseBackupRowDto(
            userId = request.userId,
            backupJson = request.backupJson,
            exportedAtMillis = request.exportedAtMillis,
            schemaVersion = request.schemaVersion
        )

        return try {
            val response = httpClient.post("${config.normalizedProjectUrl}/rest/v1/invoice_hammer_backups") {
                applySupabaseHeaders()
                header("Prefer", "resolution=merge-duplicates,return=minimal")
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(listOf(row)))
            }
            if (response.status.isSuccess()) {
                SupabaseBackupUploadOutcome.Success
            } else {
                SupabaseBackupUploadOutcome.Failure(
                    FailureMessage(
                        "Supabase backup failed with HTTP ${response.status.value}. ${response.bodyAsText()}"
                    )
                )
            }
        } catch (e: Exception) {
            SupabaseBackupUploadOutcome.Failure(
                FailureMessage(e.message ?: "Supabase backup upload failed.")
            )
        }
    }

    override suspend fun downloadLatestBackup(userId: String): SupabaseBackupDownloadOutcome {
        if (!config.isConfigured) {
            return SupabaseBackupDownloadOutcome.NotConfigured
        }

        return try {
            val response = httpClient.get("${config.normalizedProjectUrl}/rest/v1/invoice_hammer_backups") {
                applySupabaseHeaders()
                parameter("user_id", "eq.$userId")
                parameter("select", "backup_json,exported_at_millis,schema_version")
                parameter("order", "exported_at_millis.desc")
                parameter("limit", "1")
            }
            val body = response.bodyAsText()
            if (response.status.value == 404 || body == "[]") {
                return SupabaseBackupDownloadOutcome.NotFound
            }
            if (!response.status.isSuccess()) {
                return SupabaseBackupDownloadOutcome.Failure(
                    FailureMessage(
                        "Supabase backup download failed with HTTP ${response.status.value}. $body"
                    )
                )
            }

            val rows = json.decodeFromString<List<SupabaseBackupRowResponseDto>>(body)
            val latest = rows.firstOrNull()
                ?: return SupabaseBackupDownloadOutcome.NotFound

            SupabaseBackupDownloadOutcome.Success(
                backupJson = latest.backupJson,
                exportedAtMillis = latest.exportedAtMillis
            )
        } catch (e: Exception) {
            SupabaseBackupDownloadOutcome.Failure(
                FailureMessage(e.message ?: "Supabase backup download failed.")
            )
        }
    }

    override suspend fun fetchActiveTiers(): SupabaseSubscriptionTiersOutcome {
        if (!config.isConfigured) return SupabaseSubscriptionTiersOutcome.NotConfigured
        return try {
            val response = httpClient.get("${config.normalizedProjectUrl}/rest/v1/subscription_tiers") {
                applySupabaseHeaders()
                parameter("is_active", "eq.true")
                parameter("select", "*")
                parameter("order", "sort_order.asc")
            }
            val body = response.bodyAsText()
            if (!response.status.isSuccess()) {
                return SupabaseSubscriptionTiersOutcome.Failure(
                    FailureMessage("Supabase tiers failed with HTTP ${response.status.value}. $body")
                )
            }
            SupabaseSubscriptionTiersOutcome.Success(
                json.decodeFromString<List<SupabaseSubscriptionTierDto>>(body)
            )
        } catch (e: Exception) {
            SupabaseSubscriptionTiersOutcome.Failure(
                FailureMessage(e.message ?: "Supabase tier catalog failed.")
            )
        }
    }

    override suspend fun fetchUserEntitlement(userId: String): SupabaseUserEntitlementFetchOutcome {
        if (!config.isConfigured) return SupabaseUserEntitlementFetchOutcome.NotConfigured
        return try {
            val response = httpClient.get("${config.normalizedProjectUrl}/rest/v1/user_entitlements") {
                applySupabaseHeaders()
                parameter("user_id", "eq.$userId")
                parameter("select", "*")
                parameter("limit", "1")
            }
            val body = response.bodyAsText()
            if (body == "[]" || response.status.value == 404) {
                return SupabaseUserEntitlementFetchOutcome.NotFound
            }
            if (!response.status.isSuccess()) {
                return SupabaseUserEntitlementFetchOutcome.Failure(
                    FailureMessage("Supabase entitlement fetch failed with HTTP ${response.status.value}. $body")
                )
            }
            val rows = json.decodeFromString<List<SupabaseUserEntitlementDto>>(body)
            val row = rows.firstOrNull() ?: return SupabaseUserEntitlementFetchOutcome.NotFound
            SupabaseUserEntitlementFetchOutcome.Success(row)
        } catch (e: Exception) {
            SupabaseUserEntitlementFetchOutcome.Failure(
                FailureMessage(e.message ?: "Supabase entitlement fetch failed.")
            )
        }
    }

    override suspend fun upsertUserEntitlement(
        request: SupabaseEntitlementUpsertRequest
    ): SupabaseEntitlementUpsertOutcome {
        if (!config.isConfigured) return SupabaseEntitlementUpsertOutcome.Skipped
        val row = SupabaseUserEntitlementDto(
            userId = request.userId,
            tierId = request.tierId,
            source = request.source,
            purchaseToken = request.purchaseToken,
            expiresAtMillis = request.expiresAtMillis,
            updatedAtMillis = request.updatedAtMillis
        )
        return try {
            val response = httpClient.post("${config.normalizedProjectUrl}/rest/v1/user_entitlements") {
                applySupabaseHeaders()
                header("Prefer", "resolution=merge-duplicates,return=minimal")
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(listOf(row)))
            }
            if (response.status.isSuccess()) {
                SupabaseEntitlementUpsertOutcome.Success
            } else {
                SupabaseEntitlementUpsertOutcome.Failure(
                    FailureMessage(
                        "Supabase entitlement upsert failed with HTTP ${response.status.value}. ${response.bodyAsText()}"
                    )
                )
            }
        } catch (e: Exception) {
            SupabaseEntitlementUpsertOutcome.Failure(
                FailureMessage(e.message ?: "Supabase entitlement upsert failed.")
            )
        }
    }

    private fun io.ktor.client.request.HttpRequestBuilder.applySupabaseHeaders() {
        accept(ContentType.Application.Json)
        header("apikey", config.anonKey)
        header(HttpHeaders.Authorization, "Bearer ${config.anonKey}")
        header("Accept-Profile", config.schema)
        header("Content-Profile", config.schema)
    }
}
