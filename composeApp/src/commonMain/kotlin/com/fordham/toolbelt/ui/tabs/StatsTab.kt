package com.fordham.toolbelt.ui.tabs

import androidx.compose.foundation.clickable
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
import com.fordham.toolbelt.domain.model.BusinessSettings
import com.fordham.toolbelt.domain.model.BusinessStats
import com.fordham.toolbelt.ui.components.TacticalButton
import com.fordham.toolbelt.ui.tabs.stats.*

/**
 * Responsibility: Main orchestration for the Statistics & Analytics tab.
 * ADHERENCE: Below 300 line limit.
 */
@Composable
fun StatsTab(
    stats: BusinessStats,
    settings: BusinessSettings,
    onExportCsv: () -> Unit,
    onExportZip: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onInsertStressInvoices: () -> Unit,
    onEraseAllInvoices: () -> Unit
) {
    var showLockDialog by remember { mutableStateOf(false) }

    if (showLockDialog) {
        AlertDialog(
            onDismissRequest = { showLockDialog = false },
            title = { Text("PRO FEATURE LOCKED", fontWeight = FontWeight.Black) },
            text = { Text("Bento Reporting and Tax Bundles are premium features. Upgrade to Pro in Settings to unlock these tools.") },
            confirmButton = {
                TacticalButton(
                    onClick = { 
                        showLockDialog = false
                        onNavigateToSettings()
                    },
                    text = "GO TO SETTINGS"
                )
            },
            dismissButton = {
                TextButton(onClick = { showLockDialog = false }) { Text("CANCEL") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "BUSINESS ANALYTICS",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f)
            )
            Surface(
                color = if (settings.isPremium) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    if (settings.isPremium) "PRO ACCOUNT" else "FREE ACCOUNT",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (settings.isPremium) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // BENTO GRID
        BentoGrid(stats)

        Spacer(Modifier.height(24.dp))

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(), 
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TacticalButton(
                onClick = { if (settings.isPremium) onExportCsv() else showLockDialog = true },
                text = "BENTO REPORT", 
                modifier = Modifier.weight(1f), 
                containerColor = if (settings.isPremium) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                icon = { Icon(Icons.Default.TableChart, null) }
            )
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally, 
                modifier = Modifier.clickable { onNavigateToSettings() }
            ) {
                Icon(
                    if (settings.isPremium) Icons.Default.LockOpen else Icons.Default.Lock, 
                    null, 
                    tint = if (settings.isPremium) MaterialTheme.colorScheme.primary else Color.Gray, 
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    if (settings.isPremium) "PRO" else "LOCK", 
                    style = MaterialTheme.typography.labelSmall, 
                    fontWeight = FontWeight.Black, 
                    color = if (settings.isPremium) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }

            TacticalButton(
                onClick = { if (settings.isPremium) onExportZip() else showLockDialog = true },
                text = "TAX BUNDLE", 
                modifier = Modifier.weight(1f), 
                containerColor = if (settings.isPremium) MaterialTheme.colorScheme.secondary else Color.Gray.copy(alpha = 0.5f), 
                icon = { Icon(Icons.Default.Archive, null) }
            )
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "SYSTEM DIAGNOSTICS & STRESS TESTING",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TacticalButton(
                onClick = onInsertStressInvoices,
                text = "SIMULATE 1000",
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.tertiary,
                icon = { Icon(Icons.Default.Bolt, null) }
            )
            TacticalButton(
                onClick = onEraseAllInvoices,
                text = "PURGE VAULT",
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.error,
                icon = { Icon(Icons.Default.Delete, null) }
            )
        }

        Spacer(Modifier.height(32.dp))
        Text("PROJECT PROFITABILITY", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(8.dp))
        
        stats.projectStats.forEach { project ->
            ProjectStatCard(project)
        }
        
        Spacer(Modifier.height(100.dp))
    }
}
