package com.fordham.toolbelt.ui.tabs.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fordham.toolbelt.domain.model.SupabaseConnectionMode
import com.fordham.toolbelt.domain.repository.FordhamUser
import com.fordham.toolbelt.ui.components.TacticalButton
import com.fordham.toolbelt.ui.viewmodel.CloudSyncOperation
import com.fordham.toolbelt.ui.viewmodel.SyncState
import com.fordham.toolbelt.ui.localizeUiMessage
import org.jetbrains.compose.resources.stringResource
import invoicehammer.composeapp.generated.resources.*

@Composable
fun SettingsCloudSyncSection(
    currentUser: FordhamUser?,
    syncState: SyncState,
    supabaseConnectionMode: SupabaseConnectionMode,
    isSupabaseLive: Boolean,
    isCloudBusy: Boolean,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onSync: () -> Unit,
    onRequestRestoreConfirm: () -> Unit
) {
    SettingsSection(title = stringResource(Res.string.account_cloud_sync_title)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (currentUser == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(Res.string.cloud_backup_inactive), fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(Res.string.cloud_backup_inactive_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    TacticalButton(
                        onClick = onSignIn,
                        text = stringResource(Res.string.sign_in),
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
                        Icon(
                            Icons.Default.AccountCircle,
                            null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(currentUser.displayName?.value ?: stringResource(Res.string.contractor_default), fontWeight = FontWeight.Black)
                        Text(currentUser.email?.value ?: "", style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = onSignOut) {
                        Text(stringResource(Res.string.sign_out), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(Res.string.cloud_sync), fontWeight = FontWeight.Bold)
                        val defaultBackupHint = when (supabaseConnectionMode) {
                            is SupabaseConnectionMode.Live ->
                                stringResource(Res.string.backup_desc_drive_supabase, supabaseConnectionMode.projectHost)
                            SupabaseConnectionMode.Disabled ->
                                stringResource(Res.string.backup_desc_drive)
                        }
                        val statusText = when (syncState) {
                            is SyncState.Syncing -> when (syncState.operation) {
                                CloudSyncOperation.Backup -> stringResource(Res.string.sync_status_uploading)
                                CloudSyncOperation.Restore -> stringResource(Res.string.sync_status_restoring)
                            }
                            is SyncState.Success -> when (syncState.operation) {
                                CloudSyncOperation.Backup -> stringResource(Res.string.sync_status_backup_complete)
                                CloudSyncOperation.Restore -> stringResource(Res.string.sync_status_restore_complete)
                            }
                            is SyncState.Error -> stringResource(Res.string.sync_status_error, localizeUiMessage(syncState.message))
                            else -> defaultBackupHint
                        }
                        val statusColor = when (syncState) {
                            is SyncState.Success -> MaterialTheme.colorScheme.primary
                            is SyncState.Error -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Text(statusText, style = MaterialTheme.typography.bodySmall, color = statusColor)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSupabaseLive) {
                            TacticalButton(
                                onClick = onRequestRestoreConfirm,
                                text = if (isCloudBusy && (syncState as? SyncState.Syncing)?.operation == CloudSyncOperation.Restore) {
                                    ""
                                } else {
                                    stringResource(Res.string.restore)
                                },
                                containerColor = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.height(40.dp).weight(1f),
                                enabled = !isCloudBusy,
                                icon = {
                                    if (isCloudBusy && (syncState as? SyncState.Syncing)?.operation == CloudSyncOperation.Restore) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = MaterialTheme.colorScheme.onSecondary,
                                            strokeWidth = 2.dp
                                        )
                                    }
                                }
                            )
                        }
                        TacticalButton(
                            onClick = onSync,
                            text = if (isCloudBusy && (syncState as? SyncState.Syncing)?.operation == CloudSyncOperation.Backup) {
                                ""
                            } else {
                                stringResource(Res.string.sync_now)
                            },
                            containerColor = if (syncState is SyncState.Success && (syncState as SyncState.Success).operation == CloudSyncOperation.Backup) {
                                Color(0xFF4CAF50)
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.height(40.dp).weight(1f),
                            enabled = !isCloudBusy,
                            icon = {
                                if (isCloudBusy && (syncState as? SyncState.Syncing)?.operation == CloudSyncOperation.Backup) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else if (syncState is SyncState.Success && (syncState as SyncState.Success).operation == CloudSyncOperation.Backup) {
                                    Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
