package com.fordham.toolbelt.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceInvoiceLogRedactorTest {
    @Test
    fun `transcript meta does not expose raw phone email or street number`() {
        val meta = VoiceInvoiceLogRedactor.transcriptMeta(
            "Invoice Jane at 12345 Main Street, call 404-555-1212 or jane@example.com"
        )

        assertTrue(meta.contains("len="))
        assertTrue(meta.contains("hash="))
        assertFalse(meta.contains("12345"))
        assertFalse(meta.contains("404-555-1212"))
        assertFalse(meta.contains("jane@example.com"))
    }
}
