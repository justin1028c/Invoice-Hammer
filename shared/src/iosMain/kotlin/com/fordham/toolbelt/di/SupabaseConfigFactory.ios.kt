package com.fordham.toolbelt.di

import com.fordham.toolbelt.data.remote.SupabaseConfig
import com.fordham.toolbelt.util.IosSecurityServiceProvider

/**
 * iOS reads Supabase settings from Keychain via [IosSecurityServiceProvider].
 *
 * Register these keys from Swift before `MainViewControllerKt.initKoinIos()`:
 *  - "supabase_url"              -> https://your-project.supabase.co
 *  - "supabase_anon_key"         -> anon JWT or publishable key (sb_publishable_...)
 *  - "supabase_service_role_key" -> optional backend-only service key
 *  - "supabase_schema"           -> usually "public"
 */
actual fun createDefaultSupabaseConfig(): SupabaseConfig {
    val bridge = IosSecurityServiceProvider.bridge ?: return SupabaseConfig(projectUrl = "", anonKey = "")
    return SupabaseConfig(
        projectUrl = bridge.getSecret("supabase_url").orEmpty(),
        anonKey = bridge.getSecret("supabase_anon_key").orEmpty(),
        serviceRoleKey = null,
        schema = bridge.getSecret("supabase_schema")?.takeIf { it.isNotBlank() } ?: SupabaseConfig.DEFAULT_SCHEMA
    )
}
