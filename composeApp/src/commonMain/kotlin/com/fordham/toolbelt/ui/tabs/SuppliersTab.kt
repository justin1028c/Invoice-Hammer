package com.fordham.toolbelt.ui.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fordham.toolbelt.domain.model.Supplier
import com.fordham.toolbelt.domain.model.SupplierCategory
import com.fordham.toolbelt.domain.model.SupplierId
import com.fordham.toolbelt.util.PlaceSuggestion
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import com.fordham.toolbelt.ui.components.TacticalButton
import com.fordham.toolbelt.ui.tabs.suppliers.*
import com.fordham.toolbelt.ui.viewmodel.SuppliersData
import com.fordham.toolbelt.ui.viewmodel.SuppliersOutcome
import com.fordham.toolbelt.ui.localizeUiMessage
import com.fordham.toolbelt.util.PlatformActions
import org.jetbrains.compose.resources.stringResource
import invoicehammer.composeapp.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuppliersTab(
    uiState: SuppliersOutcome,
    isAddSheetVisible: Boolean,
    placeSuggestions: List<PlaceSuggestion>,
    isReorderMode: Boolean,
    reorderList: List<SupplierUiModel>,
    onTogglePin: (Supplier) -> Unit,
    onHideSupplier: (SupplierId) -> Unit,
    onAddClick: () -> Unit,
    onDismissAdd: () -> Unit,
    onAddSupplier: (String, SupplierCategory, String, String, String, String?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearSuggestions: () -> Unit,
    onToggleReorder: (Boolean, List<SupplierUiModel>) -> Unit,
    onMoveItem: (Int, Int) -> Unit,
    onSaveOrder: () -> Unit,
    hiddenSuppliers: List<SupplierUiModel>,
    onRestoreSupplier: (SupplierId) -> Unit,
    onLogPurchase: (SupplierId, String, Double, String) -> Unit,
    onOpenStore: (String) -> Unit,
    onSnapPhoto: (String) -> Unit,
    photoUri: String?,
    platformActions: PlatformActions
) {
    var showHiddenStores by remember { mutableStateOf(false) }
    var supplierToLog by remember { mutableStateOf<Supplier?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 24.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isReorderMode) stringResource(Res.string.reorder) else stringResource(Res.string.tab_stores),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (isReorderMode) stringResource(Res.string.reorder_desc) else stringResource(Res.string.suppliers_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (isReorderMode) {
                    TacticalButton(
                        onClick = onSaveOrder,
                        text = stringResource(Res.string.done),
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledIconButton(
                            onClick = onAddClick,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color(0xFFFF6A00),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.add_supplier))
                        }
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = { 
                                if (uiState is SuppliersOutcome.Success) {
                                    val fullList = uiState.data.pinnedSuppliers + uiState.data.activeSuppliers
                                    onToggleReorder(true, fullList)
                                }
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Sort, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            if (isReorderMode) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        items = reorderList,
                        key = { _, model -> model.domain.id.value }
                    ) { index, uiModel ->
                        ReorderItem(
                            index = index,
                            itemCount = reorderList.size,
                            uiModel = uiModel,
                            onMoveUp = { if (index > 0) onMoveItem(index, index - 1) },
                            onMoveDown = { if (index < reorderList.size - 1) onMoveItem(index, index + 1) },
                            onRemove = { onHideSupplier(uiModel.domain.id) },
                            onDragMove = onMoveItem
                        )
                    }
                }
            } else {
                when (uiState) {
                    is SuppliersOutcome.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is SuppliersOutcome.Success -> {
                        val state = uiState.data
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(
                                start = 16.dp, 
                                top = 16.dp, 
                                end = 16.dp, 
                                bottom = 100.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (state.pinnedSuppliers.isNotEmpty()) {
                                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                                    SectionHeader(title = stringResource(Res.string.pinned_suppliers))
                                }
                                items(state.pinnedSuppliers) { uiModel ->
                                    SupplierTile(
                                        uiModel = uiModel,
                                        onTogglePin = { onTogglePin(uiModel.domain) },
                                        onHide = { onHideSupplier(uiModel.domain.id) },
                                        onLogExpense = { supplierToLog = uiModel.domain },
                                        platformActions = platformActions
                                    )
                                }
                            }

                            if (state.activeSuppliers.isNotEmpty()) {
                                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                                    SectionHeader(title = stringResource(Res.string.active_suppliers))
                                }
                                items(state.activeSuppliers) { uiModel ->
                                    SupplierTile(
                                        uiModel = uiModel,
                                        onTogglePin = { onTogglePin(uiModel.domain) },
                                        onHide = { onHideSupplier(uiModel.domain.id) },
                                        onLogExpense = { supplierToLog = uiModel.domain },
                                        platformActions = platformActions
                                    )
                                }
                            }

                            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                                Spacer(modifier = Modifier.height(32.dp))
                                if (hiddenSuppliers.isNotEmpty()) {
                                    TextButton(
                                        onClick = { showHiddenStores = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.VisibilityOff, null, Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(Res.string.manage_hidden_stores, hiddenSuppliers.size), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    is SuppliersOutcome.Failure -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = localizeUiMessage(uiState.error.value), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        if (showHiddenStores) {
            ModalBottomSheet(
                onDismissRequest = { showHiddenStores = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
                    Text(stringResource(Res.string.hidden_stores), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(16.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(hiddenSuppliers) { supplier ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(supplier.domain.name.uppercase(), fontWeight = FontWeight.Bold)
                                    TacticalButton(
                                        onClick = { onRestoreSupplier(supplier.domain.id) },
                                        text = stringResource(Res.string.restore)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(40.dp))
                }
            }
        }

        if (isAddSheetVisible) {
            AddSupplierBottomSheet(
                onDismiss = onDismissAdd,
                onSave = onAddSupplier,
                onSnapPhoto = { 
                    platformActions.capturePhoto { uri ->
                        uri?.let { onSnapPhoto(it) }
                    }
                },
                photoUri = photoUri,
                suggestions = placeSuggestions,
                onQueryChange = onSearchQueryChange,
                onClearSuggestions = onClearSuggestions
            )
        }

        supplierToLog?.let { supplier ->
            QuickLogPurchaseDialog(
                supplier = supplier,
                onDismiss = { supplierToLog = null },
                onSave = { amount, desc ->
                    onLogPurchase(supplier.id, supplier.name, amount, desc)
                    supplierToLog = null
                }
            )
        }
    }
}
