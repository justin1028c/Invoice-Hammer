package com.fordham.toolbelt.ui.tabs.clients

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fordham.toolbelt.domain.model.Client

/**
 * Responsibility: Display a single client in the directory list.
 */
@Composable
fun ClientDirectoryItem(
    client: Client,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
    ) { 
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(), 
            horizontalArrangement = Arrangement.SpaceBetween, 
            verticalAlignment = Alignment.CenterVertically
        ) { 
            Column(modifier = Modifier.weight(1f)) { 
                Text(client.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text(client.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) 
            }
            Row {
                IconButton(onClick = onDeleteClick) { 
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) 
                }
                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.secondary) 
            }
        } 
    }
}
