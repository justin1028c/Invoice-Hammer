package com.fordham.toolbelt.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import invoicehammer.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun PremiumLockDialog(
    onDismiss: () -> Unit,
    onOpenPaywall: () -> Unit,
    onGoToSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.pro_feature_locked), fontWeight = FontWeight.Black) },
        text = {
            Text(stringResource(Res.string.premium_lock_dialog_desc))
        },
        confirmButton = {
            TacticalButton(
                onClick = onOpenPaywall,
                text = stringResource(Res.string.view_plans)
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
        }
    )
}
