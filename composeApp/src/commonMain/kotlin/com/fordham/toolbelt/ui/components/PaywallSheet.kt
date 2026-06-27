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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import invoicehammer.composeapp.generated.resources.*
import androidx.compose.foundation.layout.width
import com.fordham.toolbelt.domain.model.subscription.PurchasableProduct
import com.fordham.toolbelt.domain.model.subscription.SubscriptionTier
import com.fordham.toolbelt.domain.model.subscription.SubscriptionTierId
import com.fordham.toolbelt.ui.theme.BrandOrange
import com.fordham.toolbelt.ui.localizeUiMessage
import com.fordham.toolbelt.ui.viewmodel.SubscriptionUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallSheet(
    uiState: SubscriptionUiState,
    onDismiss: () -> Unit,
    onPurchase: (SubscriptionTierId) -> Unit,
    onPurchaseCreditPack: (PurchasableProduct) -> Unit,
    onRestore: () -> Unit,
    onBetaUnlock: () -> Unit
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
                    Text(stringResource(Res.string.invoice_hammer_pro), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text(
                        stringResource(Res.string.paywall_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
 
            Spacer(Modifier.height(12.dp))
 
            // Beta Tester Quick Override Card
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = BrandOrange.copy(alpha = 0.08f)
                ),
                border = BorderStroke(1.dp, BrandOrange.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        stringResource(Res.string.join_beta_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = BrandOrange
                    )
                    Text(
                        stringResource(Res.string.join_beta_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TacticalButton(
                        onClick = onBetaUnlock,
                        text = stringResource(Res.string.join_beta_btn),
                        containerColor = BrandOrange,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
 
            ProFeatureRow(stringResource(Res.string.pro_feat_ai), Icons.Default.AutoAwesome)
            ProFeatureRow(stringResource(Res.string.pro_feat_ocr), Icons.Default.Receipt)
            ProFeatureRow(stringResource(Res.string.pro_feat_reports), Icons.Default.Lock)

            Spacer(Modifier.height(12.dp))

            SubscriptionValueComparisonCard()

            Spacer(Modifier.height(16.dp))

            uiState.message?.let { message ->
                Text(
                    localizeUiMessage(message),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (uiState.purchaseSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (uiState.tiers.isEmpty()) {
                Text(
                    stringResource(Res.string.loading_plans),
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
                stringResource(Res.string.or_purchase_packs),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 4.dp)
            )
 
            CreditPackCard(
                title = stringResource(Res.string.starter_pack_title),
                credits = stringResource(Res.string.credits_count_50),
                price = stringResource(Res.string.price_starter),
                description = stringResource(Res.string.starter_pack_desc),
                badgeText = null,
                enabled = !uiState.isLoading,
                onClick = { onPurchaseCreditPack(PurchasableProduct.HammerCreditPack50) }
            )
 
            CreditPackCard(
                title = stringResource(Res.string.builder_pack_title),
                credits = stringResource(Res.string.credits_count_150),
                price = stringResource(Res.string.price_builder),
                description = stringResource(Res.string.builder_pack_desc),
                badgeText = stringResource(Res.string.contractors_choice),
                enabled = !uiState.isLoading,
                onClick = { onPurchaseCreditPack(PurchasableProduct.HammerCreditPack150) }
            )
 
            CreditPackCard(
                title = stringResource(Res.string.contractor_pack_title),
                credits = stringResource(Res.string.credits_count_400),
                price = stringResource(Res.string.price_contractor),
                description = stringResource(Res.string.contractor_pack_desc),
                badgeText = stringResource(Res.string.best_value),
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
                    Text(stringResource(Res.string.restore_purchases), fontWeight = FontWeight.Bold)
                }
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    TextButton(onClick = onDismiss) { Text(stringResource(Res.string.not_now)) }
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
    val isLifetime = tier.id.value == "founder_lifetime"
    
    // Override DB placeholder pricing strings to place real costs back in their card positions
    val cleanPrice = when {
        tier.id.value == "pro_monthly" || tier.priceLabel == "Pro / month" -> "$19.99"
        tier.id.value == "pro_yearly" || tier.priceLabel == "Pro / year" -> "$159.99"
        tier.id.value == "founder_lifetime" -> "$79.99"
        else -> tier.priceLabel
    }
 
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(enabled = enabled) { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = if (isYearly || isLifetime)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (isYearly || isLifetime)
            BorderStroke(1.5.dp, BrandOrange.copy(alpha = 0.8f))
        else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    tier.displayName.uppercase(),
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth()
                )
                if (isYearly) {
                    Spacer(Modifier.height(4.dp))
                    PaywallBadgeChip(stringResource(Res.string.best_value))
                } else if (isLifetime) {
                    Spacer(Modifier.height(4.dp))
                    PaywallBadgeChip("FOUNDER PASS")
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
                when {
                    isLifetime -> {
                        Text(
                            text = "one-time",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    isYearly -> {
                        Text(
                            text = stringResource(Res.string.pro_year),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(Res.string.pro_yearly_equivalent),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    else -> {
                        Text(
                            text = stringResource(Res.string.pro_month),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
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
                stringResource(Res.string.contractor_math_title),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(Res.string.contractor_math_desc),
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
                    Text(stringResource(Res.string.pay_as_you_go_packs), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text(stringResource(Res.string.actions_per_month), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(Res.string.price_pay_as_you_go), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(2.dp))
                    Text(stringResource(Res.string.consumes_credits_desc), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
 
                // Pro Subscription Cost Column
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.pro_subscription), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(stringResource(Res.string.unlimited_ai_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(Res.string.price_pro_monthly), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(2.dp))
                    Text(stringResource(Res.string.pro_yearly_comparison), style = MaterialTheme.typography.labelSmall, color = BrandOrange, fontWeight = FontWeight.Bold)
                }
            }
 
            Spacer(Modifier.height(12.dp))
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(Res.string.save_with_pro_yearly),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 12.dp),
                    textAlign = TextAlign.Center
                )
            }
 
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(Res.string.pro_tip_deductible),
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title.uppercase(),
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth()
                )
                if (badgeText != null) {
                    Spacer(Modifier.height(4.dp))
                    PaywallBadgeChip(badgeText)
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

@Composable
private fun PaywallBadgeChip(text: String) {
    Surface(
        color = BrandOrange,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.wrapContentWidth()
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            softWrap = true
        )
    }
}
