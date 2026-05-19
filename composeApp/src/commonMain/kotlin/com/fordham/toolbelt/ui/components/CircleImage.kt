package com.fordham.toolbelt.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
fun CircleImage(
    url: String,
    modifier: Modifier = Modifier.size(40.dp)
) {
    AsyncImage(
        model = url,
        contentDescription = "Profile Image",
        modifier = modifier.clip(CircleShape),
        contentScale = ContentScale.Crop
    )
}
