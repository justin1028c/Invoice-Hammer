package com.fordham.toolbelt.ui.tabs.suppliers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import invoicehammer.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource

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
                .height(132.dp)
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
                            .size(68.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        color = Color.White.copy(alpha = 0.96f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (supplier.logoResName != null) {
                                val logoRes = when(supplier.logoResName) {
                                    "logo_home_depot" -> Res.drawable.logo_home_depot
                                    "logo_lowes" -> Res.drawable.logo_lowes
                                    "logo_ace" -> Res.drawable.logo_ace
                                    "logo_menards" -> Res.drawable.logo_menards
                                    "logo_ferguson" -> Res.drawable.logo_ferguson
                                    "logo_sherwin" -> Res.drawable.logo_sherwin
                                    "logo_grainger" -> Res.drawable.logo_grainger
                                    "logo_abc" -> Res.drawable.logo_abc
                                    "logo_graybar" -> Res.drawable.logo_graybar
                                    "logo_siteone" -> Res.drawable.logo_siteone
                                    "logo_amazon" -> Res.drawable.logo_amazon
                                    "logo_northern" -> Res.drawable.logo_northern
                                    "logo_sunbelt" -> Res.drawable.logo_sunbelt
                                    "logo_hilti" -> Res.drawable.logo_hilti
                                    "logo_mcmaster" -> Res.drawable.logo_mcmaster
                                    else -> null
                                }
                                
                                if (logoRes != null) {
                                    Image(
                                        painter = painterResource(logoRes),
                                        contentDescription = supplier.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Icon(Icons.Default.Storefront, null, tint = MaterialTheme.colorScheme.primary)
                                }
                            } else {
                                Icon(
                                    Icons.Default.Storefront, 
                                    null, 
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
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

                Spacer(modifier = Modifier.height(8.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = supplier.name.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = supplier.category.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            DropdownMenuItem(
                text = { Text("Log Expense", fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.Default.AddCard, null) },
                onClick = { onLogExpense(); showMenu = false }
            )
            DropdownMenuItem(
                text = { Text(if (supplier.isPinned) "Unpin from Top" else "Pin to Top", fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.Default.PushPin, null) },
                onClick = { onTogglePin(); showMenu = false }
            )
            DropdownMenuItem(
                text = { Text("Hide Store", fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.Default.VisibilityOff, null) },
                onClick = { onHide(); showMenu = false }
            )
        }
    }
}
