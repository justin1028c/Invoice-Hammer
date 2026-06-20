package com.fordham.toolbelt.ui

import androidx.compose.runtime.Composable
import invoicehammer.composeapp.generated.resources.Res
import invoicehammer.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

object InvoiceCategories {
    val englishKeys = listOf(
        "Drywall",
        "Flooring",
        "Roofing",
        "Plumbing",
        "Electrical",
        "Painting",
        "Carpentry",
        "General Repair"
    )
}

@Composable
fun localizeInvoiceCategory(englishKey: String): String = when (englishKey) {
    "Drywall" -> stringResource(Res.string.category_drywall)
    "Flooring" -> stringResource(Res.string.category_flooring)
    "Roofing" -> stringResource(Res.string.category_roofing)
    "Plumbing" -> stringResource(Res.string.category_plumbing)
    "Electrical" -> stringResource(Res.string.category_electrical)
    "Painting" -> stringResource(Res.string.category_painting)
    "Carpentry" -> stringResource(Res.string.category_carpentry)
    "General Repair" -> stringResource(Res.string.category_general_repair)
    else -> englishKey
}
