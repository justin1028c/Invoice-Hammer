package com.fordham.toolbelt.di

import com.fordham.toolbelt.data.remote.SupabaseConfig
import com.fordham.toolbelt.shared.BuildConfig

actual fun createDefaultSupabaseConfig(): SupabaseConfig {
    return SupabaseConfig(
        projectUrl = BuildConfig.SUPABASE_URL,
        anonKey = BuildConfig.SUPABASE_ANON_KEY,
        serviceRoleKey = null,
        schema = BuildConfig.SUPABASE_SCHEMA.ifBlank { SupabaseConfig.DEFAULT_SCHEMA }
    )
}
