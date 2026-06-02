package com.fordham.toolbelt.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import com.fordham.toolbelt.ui.components.PremiumLockDialog
import com.fordham.toolbelt.ui.components.TacticalButton
import com.fordham.toolbelt.ui.viewmodel.NewInvoiceUiState

/**
 * Responsibility: Manage global-level dialogs for the MainScreen (Premium locks, AI confirmation, Errors).
 */
@Composable
fun MainScreenDialogs(
    newInvoiceUiState: NewInvoiceUiState,
    showPremiumLock: Boolean,
    statsError: String?,
    agentError: String?,
    onDismissPremium: () -> Unit,
    onOpenPaywall: () -> Unit,
    onGoToSettings: () -> Unit,
    onDismissAiConf: () -> Unit,
    onAcceptAi: () -> Unit,
    onDismissNewInvoiceError: () -> Unit,
    onDismissStatsError: () -> Unit,
    onDismissAgentError: () -> Unit
) {
    if (showPremiumLock) {
        PremiumLockDialog(
            onDismiss = onDismissPremium,
            onOpenPaywall = onOpenPaywall,
            onGoToSettings = onGoToSettings
        )
    }

    if (newInvoiceUiState.showAiConf) {
        AlertDialog(
            onDismissRequest = onDismissAiConf,
            title = { Text("Confirm AI", fontWeight = FontWeight.Black) },
            text = {
                Column {
                    newInvoiceUiState.pendingAi.forEach {
                        Text("• " + it.description + " ($" + it.amount + ")", fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = { TacticalButton(onClick = onAcceptAi, text = "ADD ALL") },
            dismissButton = { TextButton(onClick = onDismissAiConf) { Text("CANCEL") } }
        )
    }

    newInvoiceUiState.errorMessage?.let { error ->
        AlertDialog(
            onDismissRequest = onDismissNewInvoiceError,
            title = { Text("ERROR", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error) },
            text = { Text(error) },
            confirmButton = { TacticalButton(onClick = onDismissNewInvoiceError, text = "OK") }
        )
    }

    statsError?.let { error ->
        AlertDialog(
            onDismissRequest = onDismissStatsError,
            title = { Text("REPORT ERROR", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error) },
            text = { Text(error) },
            confirmButton = { TacticalButton(onClick = onDismissStatsError, text = "OK") }
        )
    }

    agentError?.let { error ->
        AlertDialog(
            onDismissRequest = onDismissAgentError,
            title = { Text("AI SYSTEM ERROR", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error) },
            text = { Text(error) },
            confirmButton = { TacticalButton(onClick = onDismissAgentError, text = "DISMISS") }
        )
    }
}
