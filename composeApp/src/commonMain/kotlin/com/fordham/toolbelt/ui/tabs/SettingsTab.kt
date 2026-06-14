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

@Composable
fun SettingsTab(
    settings: BusinessSettings,
    currentUser: com.fordham.toolbelt.domain.repository.FordhamUser?,
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

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text("RESTORE FROM SUPABASE?", fontWeight = FontWeight.Black) },
            text = {
                Text(
                    "This replaces local clients, invoices, receipts, suppliers, and business settings with your latest Supabase backup. Job photos and payment ledger entries are not restored.",
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirm = false
                        onRestore()
                    }
                ) { Text("RESTORE", fontWeight = FontWeight.Black) }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) { Text("CANCEL") }
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
            "APPLICATION SETTINGS",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )
        
        Spacer(Modifier.height(24.dp))

        SettingsSection(title = "INVOICE REMINDERS") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Unpaid invoice reminders",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Daily notification with up to three unpaid invoices (not estimates). First reminder ~15 minutes after enabling, then every 24 hours.",
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

        SettingsSection(title = "BUSINESS BRANDING") {
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
        SettingsSection(title = "AI ASSISTANT") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Direct Invoice Saving",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "When enabled, voice commands to create an invoice will automatically prompt to approve and save it, skipping the draft staging area.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Precautions: Ensure your voice commands contain all required details (client, description, pricing) to avoid saving incomplete invoices.",
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

        // APPEARANCE SECTION
        SettingsSection(title = "APPEARANCE") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Dark Mode", fontWeight = FontWeight.Bold)
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
        SettingsSection(title = "SECURITY") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Biometric Lock", fontWeight = FontWeight.Bold)
                    Text(
                        "Require fingerprint or passcode to unlock Invoice Hammer on launch. (Screenshots and recordings are never blocked).",
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
                                platformActions.showToast("Biometrics not available on this device")
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
        SettingsSection(title = "PRIVACY & PERMISSIONS") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Sensitive Permissions Disclosure", 
                    style = MaterialTheme.typography.titleSmall, 
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Invoice Hammer requires the following permissions to function. We only access these when you actively use the corresponding features:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                PermissionItem(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    description = "Used for daily unpaid invoice reminders. You can turn these off in Invoice Reminders at the top of Settings."
                )

                PermissionItem(
                    icon = Icons.Default.Mic,
                    title = "Microphone Access",
                    description = "Used for the AI Command Center to process voice commands. No audio is stored outside of active processing."
                )
                
                PermissionItem(
                    icon = Icons.Default.CameraAlt,
                    title = "Camera Access",
                    description = "Used for scanning receipts and capturing job photos. Images are stored locally in your directory."
                )
                
                PermissionItem(
                    icon = Icons.Default.Storage,
                    title = "File Storage",
                    description = "Used to save, export, and manage your invoices, estimates, and PDF reports."
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Spacer(Modifier.height(32.dp))

        TacticalButton(
            onClick = { 
                onSaveSettings(tempSettings)
                platformActions.showToast("Settings Saved")
            },
            text = "SAVE ALL CHANGES",
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.primary
        )
        
        Spacer(Modifier.height(100.dp))
    }
}
