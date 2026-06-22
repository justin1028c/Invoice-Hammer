package com.fordham.toolbelt.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fordham.toolbelt.ui.theme.*
import com.fordham.toolbelt.domain.model.BusinessSettings
import com.fordham.toolbelt.ui.components.BusinessLogoSection
import com.fordham.toolbelt.ui.components.TacticalButton
import com.fordham.toolbelt.domain.model.stripe.StripeConnectSetupState
import com.fordham.toolbelt.domain.model.stripe.StripePaymentMode
import com.fordham.toolbelt.ui.tabs.settings.SettingsCloudSyncSection
import com.fordham.toolbelt.ui.tabs.settings.SettingsSection
import com.fordham.toolbelt.ui.tabs.settings.SettingsStripeConnectSection
import com.fordham.toolbelt.ui.tabs.settings.SettingsSubscriptionSection
import com.fordham.toolbelt.ui.tabs.settings.PermissionItem
import com.fordham.toolbelt.domain.model.SupabaseConnectionMode
import com.fordham.toolbelt.ui.viewmodel.CloudSyncOperation
import com.fordham.toolbelt.ui.viewmodel.SyncState
import com.fordham.toolbelt.util.Permission
import com.fordham.toolbelt.util.PlatformActions
import org.jetbrains.compose.resources.stringResource
import invoicehammer.composeapp.generated.resources.*

@Composable
fun SettingsTab(
    settings: BusinessSettings,
    currentUser: com.fordham.toolbelt.domain.repository.FordhamUser?,
    isLlamaDownloaded: Boolean,
    isLlamaDownloading: Boolean,
    llamaDownloadProgress: Float,
    onDownloadLlama: () -> Unit,
    onDeleteLlama: () -> Unit,
    onSaveSettings: (BusinessSettings) -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onSync: () -> Unit,
    onRestore: () -> Unit,
    onOpenPaywall: () -> Unit = {},
    isPro: Boolean = false,
    syncState: SyncState,
    supabaseConnectionMode: SupabaseConnectionMode = SupabaseConnectionMode.Disabled,
    stripePaymentMode: StripePaymentMode = StripePaymentMode.ManualEntrySimulator,
    stripeConnectState: StripeConnectSetupState = StripeConnectSetupState.BackendDisabled,
    stripeConnectBusy: Boolean = false,
    onRefreshStripeConnect: () -> Unit = {},
    onStartStripeConnectOnboarding: () -> Unit = {},
    platformActions: PlatformActions,
    onPickBusinessLogo: () -> Unit,
    onRemoveBusinessLogo: () -> Unit
) {
    var tempSettings by remember(settings) { mutableStateOf(settings) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val isDarkMode = MaterialTheme.colorScheme.background == Color(0xFF000000)
    val isSupabaseLive = supabaseConnectionMode is SupabaseConnectionMode.Live
    val isCloudBusy = syncState is SyncState.Syncing
    val biometricsNotAvailableText = stringResource(Res.string.biometrics_not_available)
    val settingsSavedText = stringResource(Res.string.settings_saved_toast)

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text(stringResource(Res.string.restore_supabase), fontWeight = FontWeight.Black) },
            text = {
                Text(
                    stringResource(Res.string.confirm_restore_full_desc),
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirm = false
                        onRestore()
                    }
                ) { Text(stringResource(Res.string.restore), fontWeight = FontWeight.Black) }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) { Text(stringResource(Res.string.cancel)) }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            stringResource(Res.string.application_settings),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )
        
        Spacer(Modifier.height(24.dp))

        SettingsSection(title = stringResource(Res.string.invoice_reminders_title)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(Res.string.unpaid_reminders_label),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        stringResource(Res.string.unpaid_reminders_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = tempSettings.notificationsEnabled,
                    onCheckedChange = { enabled ->
                        val apply = {
                            tempSettings = tempSettings.copy(notificationsEnabled = enabled)
                            onSaveSettings(tempSettings)
                        }
                        if (enabled) {
                            platformActions.requestPermission(Permission.POST_NOTIFICATIONS) {
                                apply()
                            }
                        } else {
                            apply()
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = BrandOrange,
                        checkedBorderColor = BrandOrange,
                        uncheckedThumbColor = if (isDarkMode) Color.Gray else Color.White,
                        uncheckedTrackColor = if (isDarkMode) Color(0xFF222222) else Color(0xFFCCCCCC),
                        uncheckedBorderColor = if (isDarkMode) Color(0xFF444444) else Color(0xFF999999)
                    )
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        SettingsCloudSyncSection(
            currentUser = currentUser,
            syncState = syncState,
            supabaseConnectionMode = supabaseConnectionMode,
            isSupabaseLive = isSupabaseLive,
            isCloudBusy = isCloudBusy,
            onSignIn = onSignIn,
            onSignOut = onSignOut,
            onSync = onSync,
            onRequestRestoreConfirm = { showRestoreConfirm = true }
        )

        Spacer(Modifier.height(16.dp))

        SettingsStripeConnectSection(
            stripePaymentMode = stripePaymentMode,
            connectState = stripeConnectState,
            connectBusy = stripeConnectBusy,
            currentUser = currentUser,
            onRefreshConnectStatus = onRefreshStripeConnect,
            onStartConnectOnboarding = onStartStripeConnectOnboarding
        )

        Spacer(Modifier.height(16.dp))

        SettingsSection(title = stringResource(Res.string.business_branding_title)) {
            BusinessLogoSection(
                logoUri = tempSettings.logoUri,
                onPickLogo = onPickBusinessLogo,
                onRemoveLogo = onRemoveBusinessLogo
            )
        }

        Spacer(Modifier.height(16.dp))

        SettingsSubscriptionSection(
            tempSettings = tempSettings,
            isPro = isPro,
            isDarkMode = isDarkMode,
            onOpenPaywall = onOpenPaywall,
            onSaveSettings = onSaveSettings,
            onTempSettingsChange = { tempSettings = it }
        )

        Spacer(Modifier.height(16.dp))

        // AI ASSISTANT SECTION
        SettingsSection(title = stringResource(Res.string.ai_assistant_title)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(Res.string.direct_invoice_saving_label),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(Res.string.direct_invoice_saving_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(Res.string.direct_invoice_saving_warn),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(Modifier.width(16.dp))
                Switch(
                    checked = tempSettings.autoSaveVoiceInvoices,
                    onCheckedChange = { enabled ->
                        val newSettings = tempSettings.copy(autoSaveVoiceInvoices = enabled)
                        tempSettings = newSettings
                        onSaveSettings(newSettings)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = BrandOrange,
                        checkedBorderColor = BrandOrange,
                        uncheckedThumbColor = if (isDarkMode) Color.Gray else Color.White,
                        uncheckedTrackColor = if (isDarkMode) Color(0xFF222222) else Color(0xFFCCCCCC),
                        uncheckedBorderColor = if (isDarkMode) Color(0xFF444444) else Color(0xFF999999)
                    )
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // OFFLINE AI (LLAMA 3.2) SECTION
        SettingsSection(title = "Offline AI (Llama 3.2)") {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Download the Llama 3.2 3B model (approx. 1.8GB) to enable fully offline local invoice parsing and voice assistant capabilities.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                
                if (isLlamaDownloaded) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Downloaded",
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Model Downloaded",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                        TextButton(onClick = onDeleteLlama) {
                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Delete Model", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }
                } else if (isLlamaDownloading) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Downloading model...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${(llamaDownloadProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { llamaDownloadProgress },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Offline Model Status: Not Cached",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TacticalButton(
                            onClick = onDownloadLlama,
                            text = "Download (1.8GB)",
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // API COST SECTION (Google AI Key Developer Costs)
        SettingsSection(title = stringResource(Res.string.settings_api_cost_title)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(Res.string.settings_gemini_usage_label),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(Res.string.settings_gemini_usage_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    text = "$${com.fordham.toolbelt.util.DateTimeUtil.formatDecimal(tempSettings.cumulativeLlmCostUsd, 4)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // APPEARANCE SECTION
        SettingsSection(title = stringResource(Res.string.appearance_title)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(Res.string.dark_mode), fontWeight = FontWeight.Bold)
                Switch(
                    checked = tempSettings.isDarkMode,
                    onCheckedChange = { 
                        val newSettings = tempSettings.copy(isDarkMode = it)
                        tempSettings = newSettings
                        onSaveSettings(newSettings)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = BrandOrange,
                        checkedBorderColor = BrandOrange,
                        uncheckedThumbColor = if (isDarkMode) Color.Gray else Color.White,
                        uncheckedTrackColor = if (isDarkMode) Color(0xFF222222) else Color(0xFFCCCCCC),
                        uncheckedBorderColor = if (isDarkMode) Color(0xFF444444) else Color(0xFF999999)
                    )
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // SECURITY SECTION (Biometric Lock Toggle)
        SettingsSection(title = stringResource(Res.string.security_title)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.biometric_lock), fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(Res.string.biometric_lock_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(16.dp))
                Switch(
                    checked = tempSettings.biometricLockEnabled,
                    onCheckedChange = { enabled ->
                        val apply = {
                            tempSettings = tempSettings.copy(biometricLockEnabled = enabled)
                            onSaveSettings(tempSettings)
                        }
                        if (enabled) {
                            if (platformActions.isBiometricAvailable()) {
                                apply()
                             } else {
                                platformActions.showToast(biometricsNotAvailableText)
                            }
                        } else {
                            apply()
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = BrandOrange,
                        checkedBorderColor = BrandOrange,
                        uncheckedThumbColor = if (isDarkMode) Color.Gray else Color.White,
                        uncheckedTrackColor = if (isDarkMode) Color(0xFF222222) else Color(0xFFCCCCCC),
                        uncheckedBorderColor = if (isDarkMode) Color(0xFF444444) else Color(0xFF999999)
                    )
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // PRIVACY & PERMISSIONS SECTION (Google Policy Compliance)
        SettingsSection(title = stringResource(Res.string.privacy_permissions_title)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(Res.string.sensitive_permissions_disclosure), 
                    style = MaterialTheme.typography.titleSmall, 
                    fontWeight = FontWeight.Bold
                )
                Text(
                    stringResource(Res.string.permissions_disclosure_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                PermissionItem(
                    icon = Icons.Default.Notifications,
                    title = stringResource(Res.string.perm_notifications),
                    description = stringResource(Res.string.perm_notifications_desc)
                )
 
                PermissionItem(
                    icon = Icons.Default.Mic,
                    title = stringResource(Res.string.perm_microphone),
                    description = stringResource(Res.string.perm_microphone_desc)
                )
                
                PermissionItem(
                    icon = Icons.Default.CameraAlt,
                    title = stringResource(Res.string.perm_camera),
                    description = stringResource(Res.string.perm_camera_desc)
                )
                
                PermissionItem(
                    icon = Icons.Default.Storage,
                    title = stringResource(Res.string.perm_storage),
                    description = stringResource(Res.string.perm_storage_desc)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Spacer(Modifier.height(32.dp))

        TacticalButton(
            onClick = { 
                onSaveSettings(tempSettings)
                platformActions.showToast(settingsSavedText)
            },
            text = stringResource(Res.string.save_all_changes),
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.primary
        )
        
        Spacer(Modifier.height(100.dp))
    }
}
