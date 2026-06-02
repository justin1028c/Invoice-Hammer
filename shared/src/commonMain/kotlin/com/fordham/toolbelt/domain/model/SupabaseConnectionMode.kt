package com.fordham.toolbelt.domain.model

sealed interface SupabaseConnectionMode {
    data object Disabled : SupabaseConnectionMode
    data class Live(val projectHost: String) : SupabaseConnectionMode
}
