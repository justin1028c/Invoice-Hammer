package com.fordham.toolbelt.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.width
import com.fordham.toolbelt.domain.model.subscription.PurchasableProduct
import com.fordham.toolbelt.domain.model.subscription.SubscriptionTier
import com.fordham.toolbelt.domain.model.subscription.SubscriptionTierId
import com.fordham.toolbelt.ui.theme.BrandOrange
import com.fordham.toolbelt.ui.viewmodel.SubscriptionUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallSheet(
    uiState: SubscriptionUiState,
    onDismiss: () -> Unit,
    onPurchase: (SubscriptionTierId) -> Unit,
    onPurchaseCreditPack: (PurchasableProduct) -> Unit,
    onRestore: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Star, null, tint = BrandOrange)
                Column {
                    Text("INVOICE HAMMER PRO", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text(
                        "Subscriptions via Google Play or App Store. Entitlements sync to Supabase.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            ProFeatureRow("AI Command Center & Foreman agent", Icons.Default.AutoAwesome)
            ProFeatureRow("Receipt OCR scanning", Icons.Default.Receipt)
            ProFeatureRow("Bento reports & tax bundle export", Icons.Default.Lock)

            Spacer(Modifier.height(12.dp))

            SubscriptionValueComparisonCard()

            Spacer(Modifier.height(16.dp))

            uiState.message?.let { message ->
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (uiState.purchaseSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (uiState.tiers.isEmpty()) {
                Text(
                    "Loading plans from Supabase…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                uiState.tiers.forEach { tier ->
                    PaywallTierCard(
                        tier = tier,
                        enabled = !uiState.isLoading,
                        onClick = { onPurchase(tier.id) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "OR PURCHASE HAMMER CREDIT PACKS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            CreditPackCard(
                title = "Starter Pack",
                credits = "50 Credits",
                price = "$4.99",
                description = "Perfect for standard contractor invoicing & AI scans.",
                badgeText = null,
                enabled = !uiState.isLoading,
                onClick = { onPurchaseCreditPack(PurchasableProduct.HammerCreditPack50) }
            )

            CreditPackCard(
                title = "Builder Pack",
                credits = "150 Credits",
                price = "$9.99",
                description = "Most Popular. Designed for growing handyman operations.",
                badgeText = "Contractor's Choice",
                enabled = !uiState.isLoading,
                onClick = { onPurchaseCreditPack(PurchasableProduct.HammerCreditPack150) }
            )

            CreditPackCard(
                title = "Contractor Pack",
                credits = "400 Credits",
                price = "$19.99",
                description = "Best Value. Complete enterprise utility access.",
                badgeText = "Best Value",
                enabled = !uiState.isLoading,
                onClick = { onPurchaseCreditPack(PurchasableProduct.HammerCreditPack400) }
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onRestore, enabled = !uiState.isLoading) {
                    Text("RESTORE PURCHASES", fontWeight = FontWeight.Bold)
                }
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    TextButton(onClick = onDismiss) { Text("NOT NOW") }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProFeatureRow(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, null, tint = BrandOrange, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PaywallTierCard(
    tier: SubscriptionTier,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val isYearly = tier.id.value == "pro_yearly"
    
    // Override DB placeholder pricing strings to place real costs back in their card positions
    val cleanPrice = when {
        tier.id.value == "pro_monthly" || tier.priceLabel == "Pro / month" -> "$19.99"
        tier.id.value == "pro_yearly" || tier.priceLabel == "Pro / year" -> "$159.99"
        else -> tier.priceLabel
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(enabled = enabled) { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = if (isYearly)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (isYearly)
            BorderStroke(1.5.dp, BrandOrange.copy(alpha = 0.8f))
        else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(tier.displayName.uppercase(), fontWeight = FontWeight.Black, style = MaterialTheme.typography.bodyLarge)
                    if (isYearly) {
                        Surface(
                            color = BrandOrange,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "BEST VALUE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    tier.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = cleanPrice,
                    color = BrandOrange,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleMedium
                )
                if (isYearly) {
                    Text(
                        text = "Pro / year",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "= $13.33 / mo",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Text(
                        text = "Pro / month",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SubscriptionValueComparisonCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "🔨 CONTRACTOR MATH: SUBSCRIPTION VS. CREDIT PACKS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Active contractors run 450+ actions monthly (scans, invoices, tax bundles, and Foreman AI assistant).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Credit Pack Cost Column
                Column(modifier = Modifier.weight(1f)) {
                    Text("PAY-AS-YOU-GO PACKS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text("450 actions / mo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text("$24.98 / mo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(2.dp))
                    Text("Consumes credits for every scan & invoice", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }

                // Pro Subscription Cost Column
                Column(modifier = Modifier.weight(1f)) {
                    Text("PRO SUBSCRIPTION", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("UNLIMITED + 200 AI / mo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Text("$19.99 / mo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(2.dp))
                    Text("Or Pro Yearly: $13.33 / mo ($159.99 / yr)", style = MaterialTheme.typography.labelSmall, color = BrandOrange, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(12.dp))
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "🎉 SAVE UP TO 46% MONTHLY WITH PRO YEARLY",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 12.dp),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "💡 Pro Tip: All subscription plans and credit packs are 100% tax-deductible business expenses.",
                style = MaterialTheme.typography.bodySmall,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CreditPackCard(
    title: String,
    credits: String,
    price: String,
    description: String,
    badgeText: String?,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(enabled = enabled) { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = if (badgeText != null)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (badgeText != null)
            BorderStroke(1.5.dp, BrandOrange.copy(alpha = 0.8f))
        else null
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(title.uppercase(), fontWeight = FontWeight.Black, style = MaterialTheme.typography.bodyLarge)
                    if (badgeText != null) {
                        Surface(
                            color = BrandOrange,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                badgeText.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(price, color = BrandOrange, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                Text(
                    credits,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
