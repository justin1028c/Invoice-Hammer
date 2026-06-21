package com.fordham.toolbelt.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fordham.toolbelt.ui.theme.*
import com.fordham.toolbelt.domain.model.InvoicePaymentRequest
import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.InvoicePaymentStatus
import com.fordham.toolbelt.util.DateTimeUtil
import org.jetbrains.compose.resources.stringResource
import invoicehammer.composeapp.generated.resources.*

@Composable
fun HistoryItemCard(
    invoice: Invoice,
    paymentRequest: InvoicePaymentRequest? = null,
    onDelete: (Invoice) -> Unit,
    onTogglePaid: (Invoice) -> Unit,
    onView: (Invoice) -> Unit,
    onShare: (Invoice) -> Unit,
    onRequestDeposit: (Invoice) -> Unit,
    onRequestFullPayment: (Invoice) -> Unit,
    onSendAiReminder: (Invoice) -> Unit,
    onConvert: ((Invoice) -> Unit)? = null
) {
    val requestDepositText = stringResource(Res.string.request_deposit)
    val payLinkText = stringResource(Res.string.pay_link)
    val paidText = stringResource(Res.string.paid)
    val unpaidText = stringResource(Res.string.unpaid)
    val finalizeInvoiceText = stringResource(Res.string.finalize_invoice)
    val viewCd = stringResource(Res.string.view)
    val shareCd = stringResource(Res.string.share)
    val deleteCd = stringResource(Res.string.delete)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (invoice.isPaid) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) 
                    else MaterialTheme.colorScheme.outline
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        invoice.clientName.value.uppercase(), 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 1.sp
                    )
                    if (invoice.isEstimate) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(top = 4.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                        ) {
                            Text(
                                " ${stringResource(Res.string.estimate).uppercase()} ", 
                                style = MaterialTheme.typography.labelSmall, 
                                color = MaterialTheme.colorScheme.primary, 
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                    paymentRequest?.let { request ->
                        Surface(
                            color = BrandOrange.copy(alpha = 0.16f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(top = 4.dp),
                            border = BorderStroke(1.dp, BrandOrange.copy(alpha = 0.4f))
                        ) {
                            Text(
                                " ${request.status.localizedLabel()}: ${request.formattedAmount} ",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandOrange,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
                Text(
                    invoice.formattedTotal, 
                    style = MaterialTheme.typography.headlineSmall, 
                    color = if (invoice.isPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, 
                    fontWeight = FontWeight.Black
                )
            }
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                DateTimeUtil.formatDateForDisplay(invoice.date).uppercase(), 
                style = MaterialTheme.typography.labelSmall, 
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Text(
                invoice.itemsSummary.value, 
                style = MaterialTheme.typography.bodyMedium, 
                maxLines = 2,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
            )
            
            if (invoice.depositAmount.value > 0) {
                Text(
                    stringResource(Res.string.deposit_label, invoice.formattedDeposit),
                    style = MaterialTheme.typography.labelSmall, 
                    color = BrandOrange, 
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TacticalButton(
                    onClick = { onRequestDeposit(invoice) },
                    text = requestDepositText,
                    modifier = Modifier.heightIn(min = 40.dp).weight(1f),
                    containerColor = MaterialTheme.colorScheme.primary,
                    icon = { Icon(Icons.Default.Link, null) },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    fontSize = 11.sp
                )
                TacticalButton(
                    onClick = { onRequestFullPayment(invoice) },
                    text = payLinkText,
                    modifier = Modifier.heightIn(min = 40.dp).weight(1f),
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    fontSize = 11.sp
                )
            }

            if (!invoice.isEstimate && !invoice.isPaid) {
                Spacer(Modifier.height(8.dp))
                TacticalButton(
                    onClick = { onSendAiReminder(invoice) },
                    text = stringResource(Res.string.send_ai_reminder).uppercase(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp),
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    icon = { Icon(Icons.Default.Share, null) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (!invoice.isEstimate || invoice.isPaid) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Checkbox(
                            checked = invoice.isPaid,
                            onCheckedChange = { if (!invoice.isEstimate) onTogglePaid(invoice) },
                            enabled = !invoice.isEstimate,
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                        Text(
                            if (invoice.isPaid) paidText else unpaidText,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                            color = if (invoice.isPaid) BrandOrange else MaterialTheme.colorScheme.error
                        )
                    }
                } else if (onConvert != null) {
                    TacticalButton(
                        onClick = { onConvert(invoice) }, 
                        text = finalizeInvoiceText,
                        modifier = Modifier.heightIn(min = 40.dp).weight(1f),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 12.sp
                    )
                }
                
                Row {
                    IconButton(onClick = { onView(invoice) }) { 
                        Icon(Icons.Default.Visibility, viewCd, tint = MaterialTheme.colorScheme.onSurface) 
                    }
                    IconButton(onClick = { onShare(invoice) }) { 
                        Icon(Icons.Default.Share, shareCd, tint = MaterialTheme.colorScheme.primary) 
                    }
                    IconButton(onClick = { onDelete(invoice) }) { 
                        Icon(Icons.Default.Delete, deleteCd, tint = MaterialTheme.colorScheme.error) 
                    }
                }
            }
        }
    }

}
