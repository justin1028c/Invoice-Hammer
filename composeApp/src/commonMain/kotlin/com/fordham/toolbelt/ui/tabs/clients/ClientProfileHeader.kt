package com.fordham.toolbelt.ui.tabs.clients

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fordham.toolbelt.domain.model.Client
import invoicehammer.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/**
 * Responsibility: Display the header for a client profile with navigation and contact actions.
 */
@Composable
fun ClientProfileHeader(
    client: Client,
    onBackClick: () -> Unit,
    onCallClick: (String) -> Unit,
    onEmailClick: (String) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically, 
        horizontalArrangement = Arrangement.SpaceBetween, 
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) { 
            IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            Text(stringResource(Res.string.client_profile), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
        }
        Row { 
            if (client.phone.value.isNotEmpty()) {
                IconButton(onClick = { onCallClick(client.phone.value) }) { 
                    Icon(Icons.Default.Call, null, tint = MaterialTheme.colorScheme.secondary) 
                }
            }
            if (client.email.value.isNotEmpty()) {
                IconButton(onClick = { onEmailClick(client.email.value) }) { 
                    Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.secondary) 
                }
            }
        }
    }
}
