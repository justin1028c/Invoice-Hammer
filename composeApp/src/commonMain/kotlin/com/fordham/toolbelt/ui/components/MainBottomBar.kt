package com.fordham.toolbelt.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import com.fordham.toolbelt.ui.theme.BrandOrange
import org.jetbrains.compose.resources.stringResource
import invoicehammer.composeapp.generated.resources.*

@Composable
fun MainBottomBar(
    currentPage: Int,
    onTabSelected: (Int) -> Unit
) {
    val isDarkMode = MaterialTheme.colorScheme.background == Color(0xFF000000)
    val borderColor = if (isDarkMode) Color(0xFF333333) else Color.Black
    val borderWidth = if (isDarkMode) 1.dp else 2.dp

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding() // Respect system navigation bar insets to prevent device clipping
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .border(borderWidth, borderColor, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 6.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp), // Extremely compact tactical height
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val navItems = listOf(
                Triple(0, Icons.Default.Add, Res.string.tab_new),
                Triple(1, Icons.Default.History, Res.string.tab_past),
                Triple(2, Icons.Default.Receipt, Res.string.tab_receipts),
                Triple(3, Icons.Default.BarChart, Res.string.tab_stats),
                Triple(4, Icons.Default.Storefront, Res.string.tab_stores),
                Triple(5, Icons.Default.Person, Res.string.tab_clients),
                Triple(6, Icons.Default.Settings, Res.string.tab_settings)
            )
            navItems.forEach { (index, icon, resourceKey) ->
                val label = stringResource(resourceKey)
                val isSelected = currentPage == index
                val contentColor = if (isSelected) BrandOrange else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onTabSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label.replace("\n", " "),
                            tint = contentColor,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = label,
                            color = contentColor,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            fontSize = 8.sp,
                            lineHeight = 9.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

