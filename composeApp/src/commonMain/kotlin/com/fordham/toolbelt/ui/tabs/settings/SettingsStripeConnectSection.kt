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

@Composable
fun SettingsStripeConnectSection(
    stripePaymentMode: StripePaymentMode,
    connectState: StripeConnectSetupState,
    connectBusy: Boolean,
    currentUser: FordhamUser?,
    onRefreshConnectStatus: () -> Unit,
    onStartConnectOnboarding: () -> Unit
) {
    SettingsSection(title = "STRIPE PAYOUTS (CONNECT)") {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Accept card, Google Pay, and Apple Pay from clients. Funds settle to your bank via Stripe Connect; Invoice Hammer can take a platform fee on each charge.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            when (stripePaymentMode) {
                StripePaymentMode.ManualEntrySimulator -> {
                    StatusRow(
                        icon = Icons.Default.Warning,
                        tint = Color(0xFFFFA726),
                        title = "Demo mode",
                        subtitle = "Add stripe.publishable.key and stripe.payment.backend.url, then rebuild, to enable live checkout."
                    )
                }
                StripePaymentMode.PaymentSheet -> when {
                    currentUser == null -> {
                        StatusRow(
                            icon = Icons.Default.Warning,
                            tint = Color(0xFFFFA726),
                            title = "Sign in required",
                            subtitle = "Use Google sign-in under Account & Cloud Sync before connecting Stripe."
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
                            Text("Checking Stripe Connect status…", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    connectState is StripeConnectSetupState.Active -> {
                        StatusRow(
                            icon = Icons.Default.CheckCircle,
                            tint = Color(0xFF4CAF50),
                            title = "Ready to accept payments",
                            subtitle = buildString {
                                append("Account ")
                                append(connectState.accountId.take(12))
                                append("…")
                                if (connectState.payoutsEnabled) {
                                    append(" · Payouts enabled")
                                } else {
                                    append(" · Finish payout details in Stripe if prompted")
                                }
                            }
                        )
                        OutlinedButton(
                            onClick = onRefreshConnectStatus,
                            enabled = !connectBusy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("REFRESH STATUS")
                        }
                    }
                    connectState is StripeConnectSetupState.Incomplete -> {
                        StatusRow(
                            icon = Icons.Default.Payment,
                            tint = BrandOrange,
                            title = "Finish Stripe setup",
                            subtitle = "Complete onboarding so clients can pay you. This opens Stripe in your browser."
                        )
                        TacticalButton(
                            onClick = onStartConnectOnboarding,
                            text = if (connectBusy) "OPENING…" else "SET UP STRIPE PAYOUTS",
                            enabled = !connectBusy,
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = BrandOrange
                        )
                        TextButton(
                            onClick = onRefreshConnectStatus,
                            enabled = !connectBusy,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("Refresh status")
                        }
                    }
                    connectState is StripeConnectSetupState.Error -> {
                        StatusRow(
                            icon = Icons.Default.Warning,
                            tint = MaterialTheme.colorScheme.error,
                            title = "Could not reach payment backend",
                            subtitle = connectState.message.value
                        )
                        OutlinedButton(
                            onClick = onRefreshConnectStatus,
                            enabled = !connectBusy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("TRY AGAIN")
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
