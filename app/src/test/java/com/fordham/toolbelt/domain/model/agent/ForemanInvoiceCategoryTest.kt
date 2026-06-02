package com.fordham.toolbelt.domain.model.agent

import org.junit.Assert.assertEquals
import org.junit.Test

class ForemanInvoiceCategoryTest {
    @Test
    fun normalize_mapsDryWallAlias() {
        assertEquals("Drywall", ForemanInvoiceCategory.normalize("dry wall"))
        assertEquals("Drywall", ForemanInvoiceCategory.normalize("DRYWALL"))
    }

    @Test
    fun normalize_defaultsWhenBlank() {
        assertEquals("General Repair", ForemanInvoiceCategory.normalize(""))
        assertEquals("General Repair", ForemanInvoiceCategory.normalize("   "))
    }

    @Test
    fun normalize_titleCasesUnknownTrade() {
        assertEquals("Hvac", ForemanInvoiceCategory.normalize("hvac"))
    }
}
