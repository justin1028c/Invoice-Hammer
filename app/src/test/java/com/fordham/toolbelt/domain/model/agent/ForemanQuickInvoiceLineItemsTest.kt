package com.fordham.toolbelt.domain.model.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ForemanQuickInvoiceLineItemsTest {
    @Test
    fun fromFlatFields_buildsSingleLineWithCategoryAndTotal() {
        val items = ForemanQuickInvoiceLineItems.fromFlatFields(
            jobDescription = "Hang and finish basement drywall",
            category = "dry wall",
            totalAmount = 2400.0
        )

        assertEquals(1, items.size)
        assertEquals("Hang and finish basement drywall", items[0].description.value)
        assertEquals("Drywall", items[0].category.value)
        assertEquals(2400.0, items[0].amount, 0.001)
    }

    @Test
    fun fromFlatFields_returnsEmptyWhenNothingProvided() {
        assertTrue(ForemanQuickInvoiceLineItems.fromFlatFields().isEmpty())
    }
}
