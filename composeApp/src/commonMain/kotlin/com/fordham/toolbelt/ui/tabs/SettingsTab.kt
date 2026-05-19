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
import com.fordham.toolbelt.ui.components.TacticalButton
import com.fordham.toolbelt.ui.viewmodel.SyncState
import com.fordham.toolbelt.util.PlatformActions

@Composable
fun SettingsTab(
    settings: BusinessSettings,
    currentUser: com.fordham.toolbelt.domain.repository.FordhamUser?,
    onSaveSettings: (BusinessSettings) -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onSync: () -> Unit,
    syncState: SyncState,
    platformActions: PlatformActions
) {
    var tempSettings by remember(settings) { mutableStateOf(settings) }
    val scrollState = rememberScrollState()
    val isDarkMode = MaterialTheme.colorScheme.background == Color(0xFF000000)

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

        // ACCOUNT & CLOUD SECTION
        SettingsSection(title = "ACCOUNT & CLOUD SYNC") {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (currentUser == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Cloud Backup Inactive", fontWeight = FontWeight.Bold)
                            Text("Sign in with Google to enable cloud backup and syncing.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        TacticalButton(
                            onClick = onSignIn,
                            text = "SIGN IN",
                            containerColor = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.height(40.dp)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val photoUrl = currentUser.photoUrl
                        if (photoUrl != null) {
                            com.fordham.toolbelt.ui.components.CircleImage(
                                url = photoUrl.value,
                                modifier = Modifier.size(48.dp)
                            )
                        } else {
                            Icon(Icons.Default.AccountCircle, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                        
                        Spacer(Modifier.width(12.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(currentUser.displayName?.value ?: "Contractor", fontWeight = FontWeight.Black)
                            Text(currentUser.email?.value ?: "", style = MaterialTheme.typography.bodySmall)
                        }
                        
                        TextButton(onClick = onSignOut) {
                            Text("SIGN OUT", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Google Drive Backup", fontWeight = FontWeight.Bold)
                            val statusText = when (syncState) {
                                is SyncState.Syncing -> "Uploading encrypted app backup..."
                                is SyncState.Success -> "Backup complete!"
                                is SyncState.Error -> "Error: ${syncState.message}"
                                else -> "Backs up app data to your private Drive app folder."
                            }
                            val statusColor = when (syncState) {
                                is SyncState.Success -> MaterialTheme.colorScheme.primary
                                is SyncState.Error -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Text(statusText, style = MaterialTheme.typography.bodySmall, color = statusColor)
                        }
                        TacticalButton(
                            onClick = onSync,
                            text = if (syncState is SyncState.Syncing) "" else "SYNC NOW",
                            containerColor = if (syncState is SyncState.Success) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.height(40.dp),
                            enabled = syncState !is SyncState.Syncing,
                            icon = {
                                if (syncState is SyncState.Syncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else if (syncState is SyncState.Success) {
                                    Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                                }
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // PREMIUM SECTION
        SettingsSection(title = "PRO FEATURES") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Pro Premium Status", fontWeight = FontWeight.Bold)
                    Text(
                        if (tempSettings.isPremium) "UNLOCKED: Bento Reports & AI Control Center active" 
                        else "LOCKED: Basic functionality only",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (tempSettings.isPremium) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
                Switch(
                    checked = tempSettings.isPremium,
                    onCheckedChange = { 
                        val newSettings = tempSettings.copy(isPremium = it)
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

@Composable
fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
fun PermissionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp).padding(top = 2.dp)
        )
        Column {
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
