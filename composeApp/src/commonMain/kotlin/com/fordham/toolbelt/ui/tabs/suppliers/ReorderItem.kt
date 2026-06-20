package com.fordham.toolbelt.ui.tabs.suppliers

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import invoicehammer.composeapp.generated.resources.*
import kotlin.math.roundToInt

/**
 * Responsibility: Display a supplier item in reorder mode with drag-to-sort capabilities.
 */
@Composable
fun androidx.compose.foundation.lazy.LazyItemScope.ReorderItem(
    index: Int,
    itemCount: Int,
    uiModel: SupplierUiModel,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onDragMove: (Int, Int) -> Unit
) {
    var offsetY by remember { mutableStateOf(0f) }
    val supplier = uiModel.domain
    val moveThresholdPx = with(LocalDensity.current) { 72.dp.toPx() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(0, offsetY.roundToInt()) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.DragHandle,
                contentDescription = stringResource(Res.string.drag_to_reorder),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(32.dp)
                    .pointerInput(index, itemCount, moveThresholdPx) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { offsetY = 0f },
                            onDragEnd = {
                                val targetIndex = (index + (offsetY / moveThresholdPx).roundToInt())
                                    .coerceIn(0, itemCount - 1)
                                if (targetIndex != index) {
                                    onDragMove(index, targetIndex)
                                }
                                offsetY = 0f
                            },
                            onDragCancel = { offsetY = 0f },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                offsetY += dragAmount.y
                            }
                        )
                    }
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(supplier.name.uppercase(), fontWeight = FontWeight.Bold)
                Text(localizeSupplierCategory(supplier.category), style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
