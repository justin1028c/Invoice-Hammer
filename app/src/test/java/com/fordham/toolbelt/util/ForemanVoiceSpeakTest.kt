package com.fordham.toolbelt.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ForemanVoiceSpeakTest {
    @Test
    fun `blank text is not spoken`() {
        assertNull(ForemanVoiceSpeak.prepareSpokenText("   "))
    }

    @Test
    fun `collapses newlines`() {
        assertEquals(
            "Opened NEW. Done.",
            ForemanVoiceSpeak.prepareSpokenText("Opened NEW.\n\nDone.")
        )
    }

    @Test
    fun `long text is trimmed at sentence boundary when possible`() {
        val long = "A".repeat(50) + ". " + "B".repeat(200)
        val spoken = ForemanVoiceSpeak.prepareSpokenText(long)!!
        assertTrue(spoken.length <= ForemanVoiceSpeak.MAX_SPEAK_CHARS + 4)
    }
}
