package com.fordham.toolbelt.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight

@Composable
fun PremiumLockDialog(
    onDismiss: () -> Unit,
    onOpenPaywall: () -> Unit,
    onGoToSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("PRO FEATURE LOCKED", fontWeight = FontWeight.Black) },
        text = {
            Text(
                "Subscribe via Google Play or the App Store to unlock AI Command Center, receipt OCR, Bento reports, and tax exports. Entitlements sync to Supabase."
            )
        },
        confirmButton = {
            TacticalButton(
                onClick = onOpenPaywall,
                text = "VIEW PLANS"
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}
