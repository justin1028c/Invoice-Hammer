package com.fordham.toolbelt.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceInvoiceIssueCatalogTest {
    @Test
    fun `classifies blocking issues`() {
        val issue = VoiceInvoiceIssueCatalog.classify("MISSING_CLIENT_NAME")

        assertEquals(VoiceInvoiceIssueSeverity.Block, issue.severity)
        assertEquals("I need the client name before I can make the invoice.", issue.message)
    }

    @Test
    fun `classifies confirmation issues`() {
        val issue = VoiceInvoiceIssueCatalog.classify("UNMATCHED_MONEY_AMOUNT")

        assertEquals(VoiceInvoiceIssueSeverity.NeedsConfirmation, issue.severity)
    }

    @Test
    fun `classifies repairable item description issues`() {
        val issue = VoiceInvoiceIssueCatalog.classify("ITEM_0_DESCRIPTION_TOO_SHORT")

        assertEquals(VoiceInvoiceIssueSeverity.Repairable, issue.severity)
    }

    @Test
    fun `classifies absurd item values as blocking`() {
        val issue = VoiceInvoiceIssueCatalog.classify("ITEM_0_ABSURD_AMOUNT:50000.0")

        assertEquals(VoiceInvoiceIssueSeverity.Block, issue.severity)
    }
}
