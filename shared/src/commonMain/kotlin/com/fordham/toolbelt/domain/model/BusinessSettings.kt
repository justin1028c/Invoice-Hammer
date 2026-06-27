package com.fordham.toolbelt.domain.model

data class BusinessSettings(
    val businessName: String = "",
    val businessSlogan: String = "",
    val businessPhone: String = "",
    val businessEmail: String = "",
    val businessAddress: String = "",
    val taxRate: Double = 0.0,
    val markupPercentage: Double = 0.0,
    val logoUri: String? = null,
    val isPremium: Boolean = false,
    val isDarkMode: Boolean = true,
    val useMetricUnits: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val biometricLockEnabled: Boolean = false,
    val hammerCredits: Int = 0,
    val aiActionsUsedThisMonth: Int = 0,
    val autoSaveVoiceInvoices: Boolean = false,
    val cumulativeLlmCostUsd: Double = 0.0,
    val hasSeenPreLaunchPaywall: Boolean = false
)

