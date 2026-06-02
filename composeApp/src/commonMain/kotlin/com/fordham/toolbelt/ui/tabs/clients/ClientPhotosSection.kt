package com.fordham.toolbelt.ui.tabs.clients

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fordham.toolbelt.domain.model.JobPhoto
import com.fordham.toolbelt.domain.model.JobPhotoPhase
import com.fordham.toolbelt.ui.components.JobPhotoCaptureButtons

/**
 * Responsibility: Display a horizontal carousel of photos linked to the client's jobs.
 */
@Composable
fun ClientPhotosSection(
    jobPhotos: List<JobPhoto>,
    canCapture: Boolean,
    onCapturePhoto: (JobPhotoPhase) -> Unit
) {
    Column {
        Text("JOB PHOTOS", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
        if (canCapture) {
            Spacer(Modifier.height(6.dp))
            JobPhotoCaptureButtons(onCapture = onCapturePhoto)
        }
        
        if (jobPhotos.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                jobPhotos.forEach { photo ->
                    Box(modifier = Modifier.size(120.dp)) {
                        Card(
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            coil3.compose.AsyncImage(
                                model = photo.localUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(6.dp),
                            color = when (photo.phase) {
                                JobPhotoPhase.Before -> MaterialTheme.colorScheme.surfaceVariant
                                JobPhotoPhase.After -> MaterialTheme.colorScheme.primary
                            },
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (photo.phase == JobPhotoPhase.Before) "BEFORE" else "AFTER",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
