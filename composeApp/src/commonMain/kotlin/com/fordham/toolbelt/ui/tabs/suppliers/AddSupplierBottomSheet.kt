package com.fordham.toolbelt.ui.tabs.suppliers

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fordham.toolbelt.domain.model.SupplierCategory
import com.fordham.toolbelt.ui.components.TacticalButton
import com.fordham.toolbelt.util.PlaceSuggestion

/**
 * Responsibility: Bottom sheet for creating a new custom supplier with place suggestions and photo capture.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSupplierBottomSheet(
    onDismiss: () -> Unit,
    onSave: (String, SupplierCategory, String, String, String, String?) -> Unit,
    onSnapPhoto: () -> Unit,
    photoUri: String?,
    suggestions: List<PlaceSuggestion>,
    onQueryChange: (String) -> Unit,
    onClearSuggestions: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var webUrl by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(SupplierCategory.HARDWARE) }
    var showSuggestions by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "NEW SUPPLIER",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { 
                    name = it
                    onQueryChange(it)
                    showSuggestions = it.isNotBlank()
                },
                label = { Text("Store Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            if (showSuggestions && suggestions.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    suggestions.forEach { suggestion ->
                        DropdownMenuItem(
                            text = { Text(suggestion.name) },
                            onClick = {
                                name = suggestion.name
                                address = suggestion.address
                                showSuggestions = false
                                onClearSuggestions()
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Address (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = webUrl,
                onValueChange = { webUrl = it },
                label = { Text("Website URL (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(24.dp))

            Text("CATEGORY", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                SupplierCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat.name) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TacticalButton(
                    onClick = onSnapPhoto,
                    text = "SNAP PHOTO",
                    containerColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                if (photoUri != null) {
                    Spacer(Modifier.width(16.dp))
                    Card(modifier = Modifier.size(60.dp), shape = RoundedCornerShape(8.dp)) {
                        coil3.compose.AsyncImage(
                            model = photoUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            TacticalButton(
                onClick = { onSave(name, category, address, phone, webUrl, photoUri) },
                text = "CREATE SUPPLIER",
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank()
            )
        }
    }
}
