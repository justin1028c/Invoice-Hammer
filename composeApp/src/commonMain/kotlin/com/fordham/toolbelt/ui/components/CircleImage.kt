package com.fordham.toolbelt.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import invoicehammer.composeapp.generated.resources.Res
import invoicehammer.composeapp.generated.resources.profile_image_cd
import org.jetbrains.compose.resources.stringResource

@Composable
fun CircleImage(
    url: String,
    modifier: Modifier = Modifier.size(40.dp)
) {
    AsyncImage(
        model = url,
        contentDescription = stringResource(Res.string.profile_image_cd),
        modifier = modifier.clip(CircleShape),
        contentScale = ContentScale.Crop
    )
}
