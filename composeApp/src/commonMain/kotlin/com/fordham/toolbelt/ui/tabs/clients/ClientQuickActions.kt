package com.fordham.toolbelt.ui.tabs.clients

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fordham.toolbelt.domain.model.Client
import org.jetbrains.compose.resources.stringResource
import invoicehammer.composeapp.generated.resources.*

/**
 * Responsibility: Renders a premium, scrollable row of tactile quick action cards for the client.
 */
@Composable
fun ClientQuickActions(
    client: Client,
    hasInvoices: Boolean,
    hasLastPdf: Boolean,
    onNewInvoiceClick: () -> Unit,
    onDuplicateLastClick: () -> Unit,
    onLastInvoiceClick: () -> Unit,
    onAddNoteClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onCallClick: (String) -> Unit,
    onEmailClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // --- NEW INVOICE ---
        QuickActionCard(
            icon = Icons.Default.Add,
            label = stringResource(Res.string.action_new_invoice),
            color = MaterialTheme.colorScheme.primary,
            onClick = onNewInvoiceClick
        )

        // --- EDIT PROFILE ---
        QuickActionCard(
            icon = Icons.Default.Edit,
            label = stringResource(Res.string.action_edit_info),
            color = MaterialTheme.colorScheme.secondary,
            onClick = onEditProfileClick
        )

        // --- ADD NOTE ---
        QuickActionCard(
            icon = Icons.Default.NoteAdd,
            label = stringResource(Res.string.action_add_note),
            color = MaterialTheme.colorScheme.secondary,
            onClick = onAddNoteClick
        )

        // --- DUPLICATE LAST ---
        QuickActionCard(
            icon = Icons.Default.ContentCopy,
            label = stringResource(Res.string.action_duplicate_last),
            color = MaterialTheme.colorScheme.secondary,
            enabled = hasInvoices,
            onClick = onDuplicateLastClick
        )

        // --- VIEW LAST PDF ---
        QuickActionCard(
            icon = Icons.Default.PictureAsPdf,
            label = stringResource(Res.string.action_view_last_pdf),
            color = MaterialTheme.colorScheme.secondary,
            enabled = hasLastPdf,
            onClick = onLastInvoiceClick
        )

        // --- CALL ---
        QuickActionCard(
            icon = Icons.Default.Phone,
            label = stringResource(Res.string.action_call_client),
            color = MaterialTheme.colorScheme.secondary,
            enabled = client.phone.value.isNotEmpty(),
            onClick = { onCallClick(client.phone.value) }
        )

        // --- EMAIL ---
        QuickActionCard(
            icon = Icons.Default.Email,
            label = stringResource(Res.string.action_email_client),
            color = MaterialTheme.colorScheme.secondary,
            enabled = client.email.value.isNotEmpty(),
            onClick = { onEmailClick(client.email.value) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickActionCard(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val contentColor = if (enabled) color else Color.Gray.copy(alpha = 0.5f)
    val cardBgColor = if (enabled) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val borderStroke = if (enabled) BorderStroke(1.dp, color.copy(alpha = 0.6f)) else BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f))

    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .width(100.dp)
            .height(85.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        border = borderStroke,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    fontWeight = FontWeight.Black
                ),
                color = if (enabled) MaterialTheme.colorScheme.onSurface else Color.Gray.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}
