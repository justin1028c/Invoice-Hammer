package com.fordham.toolbelt.ui.tabs.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fordham.toolbelt.domain.model.BusinessSettings
import com.fordham.toolbelt.ui.components.TacticalButton
import com.fordham.toolbelt.ui.theme.BrandOrange

@Composable
fun SettingsSubscriptionSection(
    tempSettings: BusinessSettings,
    isPro: Boolean,
    isDarkMode: Boolean,
    onOpenPaywall: () -> Unit,
    onSaveSettings: (BusinessSettings) -> Unit,
    onTempSettingsChange: (BusinessSettings) -> Unit
) {
    SettingsSection(title = "PRO SUBSCRIPTION & BETA TEST") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (isPro) "PRO ENTITLEMENTS ACTIVE" else "FREE TIER CONTRACTOR",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = if (isPro) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        if (isPro) "AI Command Center, OCR, Bento reports, and tax exports are unlocked."
                        else "Upgrade to access intelligent voice parsing, OCR receipt scanning, and professional report bundles.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            TacticalButton(
                onClick = onOpenPaywall,
                text = if (isPro) "MANAGE PLAN" else "UPGRADE TO PRO",
                modifier = Modifier.fillMaxWidth()
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (tempSettings.isPremium) {
                        BrandOrange.copy(alpha = 0.08f)
                    } else {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    }
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (tempSettings.isPremium) BrandOrange.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (tempSettings.isPremium) Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (tempSettings.isPremium) BrandOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                "BETA TRIAL: FREE UNLOCK",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Black,
                                color = if (tempSettings.isPremium) BrandOrange else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Switch(
                            checked = tempSettings.isPremium,
                            onCheckedChange = {
                                val newSettings = tempSettings.copy(isPremium = it)
                                onTempSettingsChange(newSettings)
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
                    Text(
                        "Enable this trial override to instantly unlock all Pro features for free during beta testing. Toggle it off to test the upgrade flow, standard lock screens, and live paywalls as users will see them.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
