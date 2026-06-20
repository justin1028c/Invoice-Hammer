package com.fordham.toolbelt.ui.tabs.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fordham.toolbelt.domain.model.stripe.StripeConnectSetupState
import com.fordham.toolbelt.domain.model.stripe.StripePaymentMode
import com.fordham.toolbelt.domain.repository.FordhamUser
import com.fordham.toolbelt.ui.components.TacticalButton
import com.fordham.toolbelt.ui.theme.BrandOrange
import org.jetbrains.compose.resources.stringResource
import invoicehammer.composeapp.generated.resources.*

@Composable
fun SettingsStripeConnectSection(
    stripePaymentMode: StripePaymentMode,
    connectState: StripeConnectSetupState,
    connectBusy: Boolean,
    currentUser: FordhamUser?,
    onRefreshConnectStatus: () -> Unit,
    onStartConnectOnboarding: () -> Unit
) {
    SettingsSection(title = stringResource(Res.string.stripe_payouts_title)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(Res.string.stripe_payouts_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
 
            when (stripePaymentMode) {
                StripePaymentMode.ManualEntrySimulator -> {
                    StatusRow(
                        icon = Icons.Default.Warning,
                        tint = Color(0xFFFFA726),
                        title = stringResource(Res.string.demo_mode_title),
                        subtitle = stringResource(Res.string.demo_mode_desc)
                    )
                }
                StripePaymentMode.PaymentSheet -> when {
                    currentUser == null -> {
                        StatusRow(
                            icon = Icons.Default.Warning,
                            tint = Color(0xFFFFA726),
                            title = stringResource(Res.string.sign_in_required_title),
                            subtitle = stringResource(Res.string.sign_in_required_desc)
                        )
                    }
                    connectState is StripeConnectSetupState.Loading -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = BrandOrange
                            )
                            Text(stringResource(Res.string.checking_stripe_status), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    connectState is StripeConnectSetupState.Active -> {
                        StatusRow(
                            icon = Icons.Default.CheckCircle,
                            tint = Color(0xFF4CAF50),
                            title = stringResource(Res.string.ready_accept_payments),
                            subtitle = buildString {
                                append(stringResource(Res.string.account_prefix))
                                append(connectState.accountId.take(12))
                                append("…")
                                if (connectState.payoutsEnabled) {
                                    append(stringResource(Res.string.payouts_enabled_suffix))
                                } else {
                                    append(stringResource(Res.string.payouts_disabled_suffix))
                                }
                            }
                        )
                        OutlinedButton(
                            onClick = onRefreshConnectStatus,
                            enabled = !connectBusy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(Res.string.refresh_status))
                        }
                    }
                    connectState is StripeConnectSetupState.Incomplete -> {
                        StatusRow(
                            icon = Icons.Default.Payment,
                            tint = BrandOrange,
                            title = stringResource(Res.string.finish_stripe_setup),
                            subtitle = stringResource(Res.string.finish_stripe_setup_desc)
                        )
                        TacticalButton(
                            onClick = onStartConnectOnboarding,
                            text = if (connectBusy) stringResource(Res.string.opening_progress) else stringResource(Res.string.setup_stripe_payouts),
                            enabled = !connectBusy,
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = BrandOrange
                        )
                        TextButton(
                            onClick = onRefreshConnectStatus,
                            enabled = !connectBusy,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(stringResource(Res.string.refresh_status_lc))
                        }
                    }
                    connectState is StripeConnectSetupState.Error -> {
                        StatusRow(
                            icon = Icons.Default.Warning,
                            tint = MaterialTheme.colorScheme.error,
                            title = stringResource(Res.string.payment_backend_error),
                            subtitle = connectState.message.value
                        )
                        OutlinedButton(
                            onClick = onRefreshConnectStatus,
                            enabled = !connectBusy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(Res.string.try_again))
                        }
                    }
                    else -> Unit
                }
            }
        }
    }
}

@Composable
private fun StatusRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
