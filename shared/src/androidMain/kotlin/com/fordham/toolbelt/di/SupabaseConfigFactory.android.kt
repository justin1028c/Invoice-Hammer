package com.fordham.toolbelt.di

import com.fordham.toolbelt.data.remote.SupabaseConfig
import com.fordham.toolbelt.shared.BuildConfig

actual fun createDefaultSupabaseConfig(): SupabaseConfig {
    val securityManager = runCatching {
        org.koin.core.context.GlobalContext.get().get<com.fordham.toolbelt.util.SecurityManager>()
    }.getOrNull()
    val prefs = securityManager?.getEncryptedPrefs()

    val projectUrl = prefs?.getString("supabase_url", null)?.takeIf { it.isNotBlank() }
        ?: BuildConfig.SUPABASE_URL

    val anonKey = prefs?.getString("supabase_anon_key", null)?.takeIf { it.isNotBlank() }
        ?: BuildConfig.SUPABASE_ANON_KEY

    val schema = prefs?.getString("supabase_schema", null)?.takeIf { it.isNotBlank() }
        ?: BuildConfig.SUPABASE_SCHEMA.ifBlank { SupabaseConfig.DEFAULT_SCHEMA }

    return SupabaseConfig(
        projectUrl = projectUrl,
        anonKey = anonKey,
        serviceRoleKey = null,
        schema = schema
    )
}
