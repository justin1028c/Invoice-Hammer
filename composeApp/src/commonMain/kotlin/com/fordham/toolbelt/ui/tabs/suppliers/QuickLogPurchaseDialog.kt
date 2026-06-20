package com.fordham.toolbelt.ui.tabs.suppliers

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fordham.toolbelt.domain.model.Supplier
import com.fordham.toolbelt.ui.components.TacticalButton
import org.jetbrains.compose.resources.stringResource
import invoicehammer.composeapp.generated.resources.*
 
 /**
  * Responsibility: Dialog for quickly logging a manual expense against a specific supplier.
  */
 @Composable
 fun QuickLogPurchaseDialog(
     supplier: Supplier,
     onDismiss: () -> Unit,
     onSave: (Double, String) -> Unit
 ) {
     var amount by remember { mutableStateOf("") }
     var description by remember { mutableStateOf("") }
 
     AlertDialog(
         onDismissRequest = onDismiss,
         title = { Text(stringResource(Res.string.quick_log_expense, supplier.name.uppercase()), fontWeight = FontWeight.Black) },
         text = {
             Column {
                 OutlinedTextField(
                     value = amount,
                     onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) amount = it },
                     label = { Text(stringResource(Res.string.amount_usd)) },
                     modifier = Modifier.fillMaxWidth()
                 )
                 Spacer(Modifier.height(16.dp))
                 OutlinedTextField(
                     value = description,
                     onValueChange = { description = it },
                     label = { Text(stringResource(Res.string.description_eg)) },
                     modifier = Modifier.fillMaxWidth()
                 )
             }
         },
         confirmButton = {
             TacticalButton(
                 onClick = { 
                     val valAmount = amount.toDoubleOrNull() ?: 0.0
                     onSave(valAmount, description)
                 },
                 text = stringResource(Res.string.save),
                 enabled = amount.isNotBlank()
             )
         },
         dismissButton = {
             TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
         }
     )
}
