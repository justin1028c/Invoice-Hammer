package com.fordham.toolbelt.ui.tabs.suppliers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fordham.toolbelt.util.PlatformActions
import com.fordham.toolbelt.util.DateTimeUtil
import org.jetbrains.compose.resources.stringResource
import invoicehammer.composeapp.generated.resources.*

/**
 * Responsibility: Display a single supplier in a high-fidelity tile with action menu.
 */
@Composable
fun SupplierTile(
    uiModel: SupplierUiModel,
    onTogglePin: () -> Unit,
    onHide: () -> Unit,
    onLogExpense: () -> Unit,
    platformActions: PlatformActions
) {
    val supplier = uiModel.domain
    var showMenu by remember { mutableStateOf(false) }
    
    Box {
        Card(
            onClick = { 
                platformActions.launchApp(
                    packageName = supplier.packageName, 
                    fallbackUrl = supplier.webUrl
                ) 
            },
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 132.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        color = Color.White.copy(alpha = 0.12f),
                        border = BorderStroke(
                            1.5.dp, 
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.1f))
                            )
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (supplier.customLogoPath != null) {
                                coil3.compose.AsyncImage(
                                    model = supplier.customLogoPath,
                                    contentDescription = supplier.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else if (supplier.logoResName != null) {
                                val initials = remember(supplier.name) {
                                    val words = supplier.name.split(" ", "-", "_")
                                    if (words.size >= 2) {
                                        "${words[0].firstOrNull() ?: ""}${words[1].firstOrNull() ?: ""}"
                                    } else {
                                        supplier.name.take(2)
                                    }.uppercase()
                                }
                                val gradientColors = remember(supplier.logoResName) {
                                    when (supplier.logoResName) {
                                        "logo_home_depot" -> listOf(Color(0xFFF97316), Color(0xFFEA580C))
                                        "logo_lowes" -> listOf(Color(0xFF3B82F6), Color(0xFF2563EB))
                                        "logo_ace" -> listOf(Color(0xFFEF4444), Color(0xFFDC2626))
                                        "logo_menards" -> listOf(Color(0xFF10B981), Color(0xFF059669))
                                        "logo_ferguson" -> listOf(Color(0xFF14B8A6), Color(0xFF0D9488))
                                        "logo_sherwin" -> listOf(Color(0xFF06B6D4), Color(0xFF0891B2))
                                        "logo_grainger" -> listOf(Color(0xFFEF4444), Color(0xFF991B1B))
                                        "logo_abc" -> listOf(Color(0xFF64748B), Color(0xFF475569))
                                        "logo_graybar" -> listOf(Color(0xFFF59E0B), Color(0xFFD97706))
                                        "logo_siteone" -> listOf(Color(0xFF10B981), Color(0xFF047857))
                                        "logo_amazon" -> listOf(Color(0xFFFF9900), Color(0xFF141923))
                                        "logo_northern" -> listOf(Color(0xFFEF4444), Color(0xFF475569))
                                        "logo_sunbelt" -> listOf(Color(0xFFEAB308), Color(0xFFCA8A04))
                                        "logo_hilti" -> listOf(Color(0xFFEF4444), Color(0xFFB91C1C))
                                        "logo_mcmaster" -> listOf(Color(0xFFFFEB3B), Color(0xFFFF9800))
                                        else -> listOf(Color(0xFF6B7280), Color(0xFF374151))
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            androidx.compose.ui.graphics.Brush.linearGradient(
                                                colors = gradientColors,
                                                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                                end = androidx.compose.ui.geometry.Offset(120f, 120f)
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Subtle diagonal gloss shine
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                androidx.compose.ui.graphics.Brush.linearGradient(
                                                    colors = listOf(
                                                        Color.White.copy(alpha = 0.15f),
                                                        Color.White.copy(alpha = 0.05f),
                                                        Color.Transparent,
                                                        Color.Transparent
                                                    ),
                                                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                                    end = androidx.compose.ui.geometry.Offset(90f, 90f)
                                                )
                                            )
                                    )
                                    Text(
                                        text = initials,
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 1.5.sp
                                        ),
                                        modifier = Modifier.padding(bottom = 1.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    Icons.Default.Storefront, 
                                    null, 
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }

                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.MoreVert,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = supplier.name.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            lineHeight = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = localizeSupplierCategory(supplier.category),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (supplier.analytics.totalSpendYtd > 0.0) {
                            Spacer(Modifier.height(4.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = " ${DateTimeUtil.formatMoney(supplier.analytics.totalSpendYtd)} YTD ",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 8.sp,
                                        lineHeight = 10.sp,
                                        fontWeight = FontWeight.Black
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onLogExpense,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCard,
                            contentDescription = stringResource(Res.string.log_expense),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.log_expense), fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.Default.AddCard, null) },
                onClick = { onLogExpense(); showMenu = false }
            )
            DropdownMenuItem(
                text = { Text(if (supplier.isPinned) stringResource(Res.string.unpin_top) else stringResource(Res.string.pin_top), fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.Default.PushPin, null) },
                onClick = { onTogglePin(); showMenu = false }
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.hide_store), fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.Default.VisibilityOff, null) },
                onClick = { onHide(); showMenu = false }
            )
        }
    }
}
