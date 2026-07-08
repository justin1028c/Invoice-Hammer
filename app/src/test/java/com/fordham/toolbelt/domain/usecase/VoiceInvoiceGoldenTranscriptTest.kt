package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.AiInvoiceResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceInvoiceGoldenTranscriptTest {
    private val extractEvidence = ExtractVoiceInvoiceEvidenceUseCase()
    private val parser = ParseVoiceInvoiceDeterministicallyUseCase()
    private val validator = ValidateVoiceInvoiceResultUseCase()
    private val normalizer = NormalizeVoiceInvoiceLineItemsUseCase()

    @Test
    fun `golden contractor transcripts parse into stable invoice facts`() {
        GoldenTranscripts.forEach { fixture ->
            val result = parse(fixture.transcript)

            assertEquals(fixture.name, fixture.clientName, result.clientName)
            assertEquals(fixture.name, fixture.clientAddress, result.clientAddress)
            assertEquals(fixture.name, fixture.lineItemCount, result.items.size)
            assertEquals(fixture.name, fixture.subtotal, result.items.sumOf { it.amount.value }, 0.01)
            fixture.expectedDescriptions.forEachIndexed { index, expected ->
                assertTrue(
                    "${fixture.name} expected description[$index] to contain '$expected' but was '${result.items[index].description.value}'",
                    result.items[index].description.value.contains(expected, ignoreCase = true)
                )
            }
        }
    }

    @Test
    fun `golden transcripts never produce client facing short descriptions`() {
        GoldenTranscripts.forEach { fixture ->
            val result = parse(fixture.transcript)

            assertTrue(
                fixture.name,
                result.items.all { it.description.value.trim().length >= 8 }
            )
        }
    }

    private fun parse(transcript: String): AiInvoiceResult {
        val evidence = extractEvidence(transcript)
        val parsed = parser(evidence.normalizedTranscript.ifBlank { transcript })
        assertNotNull("Expected deterministic parse for transcript: $transcript", parsed)
        return normalizer(validator(parsed!!, evidence))
    }

    private data class Fixture(
        val name: String,
        val transcript: String,
        val clientName: String,
        val clientAddress: String,
        val lineItemCount: Int,
        val subtotal: Double,
        val expectedDescriptions: List<String>
    )

    private companion object {
        val GoldenTranscripts = listOf(
            Fixture(
                name = "labor and drywall multi line",
                transcript = "let's make an invoice for Justin Fordham at 1941 Norwalk Court Jonesboro Georgia 30236 " +
                    "we did 15 ft of drywall we put mud on it we taped it and we skimmed it at $400 " +
                    "add 10 hours of labor at $50 an hour",
                clientName = "Justin Fordham",
                clientAddress = "1941 Norwalk Court Jonesboro GA 30236",
                lineItemCount = 2,
                subtotal = 900.0,
                expectedDescriptions = listOf("drywall", "labor")
            ),
            Fixture(
                name = "unit priced carpentry with deposit",
                transcript = "make an invoice for client Marcus Hill at 1189 West Briar Court Macon Georgia 31210 " +
                    "installed 42 linear feet of baseboard at 6.50 per foot repaired to nail pops at $30 each " +
                    "and apply $50 deposit already paid",
                clientName = "Marcus Hill",
                clientAddress = "1189 West Briar Court Macon GA 31210",
                lineItemCount = 2,
                subtotal = 333.0,
                expectedDescriptions = listOf("baseboard", "nail pops")
            ),
            Fixture(
                name = "noisy pressure wash and deck transcript",
                transcript = "make an invoice for Denny's Carter at 5:08 Brookstone Circle Stockbridge Georgia 30281 " +
                    "pressure wash 320 ft of driveway at 75 50 cents per square foot silver front steps for $180 " +
                    "replaced three cracked deck boards at $48 each",
                clientName = "Denny's Carter",
                clientAddress = "508 Brookstone Circle Stockbridge GA 30281",
                lineItemCount = 3,
                subtotal = 564.0,
                expectedDescriptions = listOf("driveway", "front steps", "deck boards")
            )
        )
    }
}
