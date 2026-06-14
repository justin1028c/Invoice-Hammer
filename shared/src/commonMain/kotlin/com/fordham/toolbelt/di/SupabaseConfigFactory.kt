package com.fordham.toolbelt.di

import com.fordham.toolbelt.data.remote.SupabaseConfig
import com.fordham.toolbelt.util.SecretProvider

fun createDefaultSupabaseConfig(secretProvider: SecretProvider): SupabaseConfig {
    val projectUrl = secretProvider.getSecret("supabase_url")
    val anonKey = secretProvider.getSecret("supabase_anon_key")
    val schema = secretProvider.getSecret("supabase_schema").ifBlank { SupabaseConfig.DEFAULT_SCHEMA }

    return SupabaseConfig(
        projectUrl = projectUrl,
        anonKey = anonKey,
        serviceRoleKey = null,
        schema = schema
    )
}
