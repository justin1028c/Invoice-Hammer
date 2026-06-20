package com.fordham.toolbelt.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fordham.toolbelt.domain.model.JobPhotoPhase
import invoicehammer.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun JobPhotoCaptureButtons(
    onCapture: (JobPhotoPhase) -> Unit,
    modifier: Modifier = Modifier
) {
    val beforeText = stringResource(Res.string.before)
    val afterText = stringResource(Res.string.after)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TacticalButton(
            onClick = { onCapture(JobPhotoPhase.Before) },
            text = beforeText,
            modifier = Modifier.weight(1f),
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface,
            icon = {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
            }
        )
        TacticalButton(
            onClick = { onCapture(JobPhotoPhase.After) },
            text = afterText,
            modifier = Modifier.weight(1f),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            icon = {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
            }
        )
    }
}
