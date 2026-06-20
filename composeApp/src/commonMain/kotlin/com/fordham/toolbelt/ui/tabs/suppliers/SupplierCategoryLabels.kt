package com.fordham.toolbelt.ui.tabs.suppliers

import androidx.compose.runtime.Composable
import com.fordham.toolbelt.domain.model.SupplierCategory
import invoicehammer.composeapp.generated.resources.Res
import invoicehammer.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun localizeSupplierCategory(category: SupplierCategory): String = when (category) {
    SupplierCategory.LUMBER -> stringResource(Res.string.supplier_category_lumber)
    SupplierCategory.ELECTRICAL -> stringResource(Res.string.supplier_category_electrical)
    SupplierCategory.PLUMBING -> stringResource(Res.string.supplier_category_plumbing)
    SupplierCategory.PAINT -> stringResource(Res.string.supplier_category_paint)
    SupplierCategory.ROOFING -> stringResource(Res.string.supplier_category_roofing)
    SupplierCategory.HVAC -> stringResource(Res.string.supplier_category_hvac)
    SupplierCategory.HARDWARE -> stringResource(Res.string.supplier_category_hardware)
    SupplierCategory.FASTENERS -> stringResource(Res.string.supplier_category_fasteners)
    SupplierCategory.FLOORING -> stringResource(Res.string.supplier_category_flooring)
    SupplierCategory.GENERAL_SUPPLY -> stringResource(Res.string.supplier_category_general_supply)
    SupplierCategory.OTHER -> stringResource(Res.string.supplier_category_other)
}
