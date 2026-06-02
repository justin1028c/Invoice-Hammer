package com.fordham.toolbelt.ui.tabs.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fordham.toolbelt.domain.model.BusinessSettings
import com.fordham.toolbelt.ui.components.BusinessLogoSection
import com.fordham.toolbelt.ui.components.TacticalButton

@Composable
fun BusinessProfileDialog(
    businessSettings: BusinessSettings,
    onDismiss: () -> Unit,
    onSave: (BusinessSettings) -> Unit,
    onPickLogo: () -> Unit,
    onRemoveLogo: () -> Unit
) {
    var tempSettings by remember(businessSettings) { mutableStateOf(businessSettings) }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("BUSINESS PROFILE", fontWeight = FontWeight.Black) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BusinessLogoSection(
                    logoUri = tempSettings.logoUri,
                    onPickLogo = onPickLogo,
                    onRemoveLogo = onRemoveLogo,
                    compact = true
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                OutlinedTextField(
                    value = tempSettings.businessName,
                    onValueChange = { tempSettings = tempSettings.copy(businessName = it) },
                    label = { Text("BUSINESS NAME") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = tempSettings.businessSlogan,
                    onValueChange = { tempSettings = tempSettings.copy(businessSlogan = it) },
                    label = { Text("SLOGAN") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = tempSettings.businessPhone,
                    onValueChange = { tempSettings = tempSettings.copy(businessPhone = it) },
                    label = { Text("PHONE") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = tempSettings.businessEmail,
                    onValueChange = { tempSettings = tempSettings.copy(businessEmail = it) },
                    label = { Text("EMAIL") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = tempSettings.businessAddress,
                    onValueChange = { tempSettings = tempSettings.copy(businessAddress = it) },
                    label = { Text("ADDRESS") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            TacticalButton(
                onClick = {
                    onSave(tempSettings)
                    onDismiss()
                },
                text = "SAVE"
            )
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("CANCEL")
            }
        }
    )
}
