package com.fordham.toolbelt.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.stringResource
import invoicehammer.composeapp.generated.resources.*
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
            title = { Text(stringResource(Res.string.confirm_ai), fontWeight = FontWeight.Black) },
            text = {
                Column {
                    newInvoiceUiState.pendingAi.forEach {
                        Text("• " + it.description + " ($" + it.amount + ")", fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = { TacticalButton(onClick = onAcceptAi, text = stringResource(Res.string.add_all)) },
            dismissButton = { TextButton(onClick = onDismissAiConf) { Text(stringResource(Res.string.cancel)) } }
        )
    }

    newInvoiceUiState.errorMessage?.let { error ->
        AlertDialog(
            onDismissRequest = onDismissNewInvoiceError,
            title = { Text(stringResource(Res.string.error), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error) },
            text = { Text(localizeUiMessage(error)) },
            confirmButton = { TacticalButton(onClick = onDismissNewInvoiceError, text = stringResource(Res.string.ok)) }
        )
    }

    statsError?.let { error ->
        AlertDialog(
            onDismissRequest = onDismissStatsError,
            title = { Text(stringResource(Res.string.report_error), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error) },
            text = { Text(localizeUiMessage(error)) },
            confirmButton = { TacticalButton(onClick = onDismissStatsError, text = stringResource(Res.string.ok)) }
        )
    }

    agentError?.let { error ->
        AlertDialog(
            onDismissRequest = onDismissAgentError,
            title = { Text(stringResource(Res.string.ai_system_error), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error) },
            text = { Text(localizeUiMessage(error)) },
            confirmButton = { TacticalButton(onClick = onDismissAgentError, text = stringResource(Res.string.dismiss)) }
        )
    }
}
