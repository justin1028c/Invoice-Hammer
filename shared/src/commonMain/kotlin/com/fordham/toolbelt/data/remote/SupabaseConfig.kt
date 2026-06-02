package com.fordham.toolbelt.data.remote

/**
 * Mobile Supabase REST config. [anonKey] accepts the legacy JWT anon key or the
 * newer publishable key (`sb_publishable_...`) from the Supabase dashboard.
 */
data class SupabaseConfig(
    val projectUrl: String,
    val anonKey: String,
    val serviceRoleKey: String? = null,
    val schema: String = DEFAULT_SCHEMA
) {
    val isConfigured: Boolean get() = projectUrl.isNotBlank() && anonKey.isNotBlank()
    val normalizedProjectUrl: String get() = projectUrl.trimEnd('/')

    companion object {
        const val DEFAULT_SCHEMA = "public"
    }
}
