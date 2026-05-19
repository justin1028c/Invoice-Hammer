package com.fordham.toolbelt.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight

@Composable
fun PremiumLockDialog(
    onDismiss: () -> Unit,
    onGoToSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("PRO FEATURE LOCKED", fontWeight = FontWeight.Black) },
        text = { Text("AI Command Center requires a Pro Account. Enable it in Settings to unlock AI automation and Bento reporting.") },
        confirmButton = {
            TacticalButton(
                onClick = onGoToSettings,
                text = "GO TO SETTINGS"
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}
