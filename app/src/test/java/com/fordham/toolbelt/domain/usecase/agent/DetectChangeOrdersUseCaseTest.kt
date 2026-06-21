package com.fordham.toolbelt.domain.usecase.agent

import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.model.agent.*
import com.fordham.toolbelt.domain.repository.InvoiceRepository
import com.fordham.toolbelt.domain.repository.JobNoteRepository
import com.fordham.toolbelt.domain.repository.GeminiRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class DetectChangeOrdersUseCaseTest {

    private val invoiceRepository: InvoiceRepository = mockk()
    private val jobNoteRepository: JobNoteRepository = mockk()
    private val geminiRepository: GeminiRepository = mockk()

    private val useCase = DetectChangeOrdersUseCase(
        invoiceRepository,
        jobNoteRepository,
        geminiRepository
    )

    private val invoiceId = InvoiceId("test-invoice-id")
    private val mockInvoice = Invoice(
        id = invoiceId,
        clientName = ClientName("John Doe"),
        clientAddress = ClientAddress("123 Main St"),
        date = "01/01/2026",
        totalAmount = MoneyAmount(5000.0),
        itemsSummary = ItemsSummary("Materials: 1000.0, Service: 4000.0"),
        isPaid = false,
        isEstimate = true
    )

    @Test
    fun `returns opportunities when Gemini successfully identifies change orders`() = runTest {
        coEvery { invoiceRepository.getInvoiceById(invoiceId) } returns mockInvoice

        // Serialize a budget note where materials budget is 1000
        val lineItems = listOf(
            LineItem(ItemsSummary("Copper Pipe"), MoneyAmount(1000.0), "Materials")
        )
        val budgetNoteText = SystemBudgetSerializer.serialize(5000.0, 1000.0, lineItems)
        val mockJobNotes = listOf(
            JobNote(
                id = NoteId("1"),
                clientName = "John Doe",
                text = budgetNoteText,
                timestamp = 1718841600000L,
                invoiceId = invoiceId
            ),
            JobNote(
                id = NoteId("2"),
                clientName = "John Doe",
                text = "Contractor note: Added 5 additional recessed lights at owner request.",
                timestamp = 1718845200000L,
                invoiceId = invoiceId
            )
        )
        every { jobNoteRepository.getNotesByInvoice(invoiceId) } returns flowOf(mockJobNotes)

        // Mock Gemini returning change order JSON
        val geminiJsonOutput = """
            {
              "opportunities": [
                {
                  "detectedTask": "Install 5 recessed lights",
                  "confidence": "VERY_HIGH",
                  "minPrice": 500.0,
                  "maxPrice": 750.0,
                  "recommendedItems": [
                    {
                      "description": "Install recessed lights",
                      "amount": 600.0,
                      "category": "Service"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        coEvery { 
            geminiRepository.processTask(TaskType.DETECT_CHANGE_ORDERS, any()) 
        } returns GeminiOutcome.Success(geminiJsonOutput)

        val outcome = useCase(invoiceId)
        assertTrue(outcome is DetectChangeOrdersOutcome.Success)
        val opportunities = (outcome as DetectChangeOrdersOutcome.Success).opportunities
        assertEquals(1, opportunities.size)
        val opportunity = opportunities.first()
        assertEquals("Install 5 recessed lights", opportunity.detectedTask.value)
        assertEquals(OpportunityConfidence.VERY_HIGH, opportunity.confidence)
        assertEquals(500.0, opportunity.estimatedValueRange.start, 0.01)
        assertEquals(750.0, opportunity.estimatedValueRange.endInclusive, 0.01)
        assertEquals(1, opportunity.recommendedItems.size)
        assertEquals("Install recessed lights", opportunity.recommendedItems.first().description.value)
        assertEquals(600.0, opportunity.recommendedItems.first().amount.value, 0.01)
    }

    @Test
    fun `returns empty list when there are no user logs`() = runTest {
        coEvery { invoiceRepository.getInvoiceById(invoiceId) } returns mockInvoice

        // Only system budget note, no user notes
        val lineItems = listOf(
            LineItem(ItemsSummary("Copper Pipe"), MoneyAmount(1000.0), "Materials")
        )
        val budgetNoteText = SystemBudgetSerializer.serialize(5000.0, 1000.0, lineItems)
        val mockJobNotes = listOf(
            JobNote(
                id = NoteId("1"),
                clientName = "John Doe",
                text = budgetNoteText,
                timestamp = 1718841600000L,
                invoiceId = invoiceId
            )
        )
        every { jobNoteRepository.getNotesByInvoice(invoiceId) } returns flowOf(mockJobNotes)

        val outcome = useCase(invoiceId)
        assertTrue(outcome is DetectChangeOrdersOutcome.Success)
        val opportunities = (outcome as DetectChangeOrdersOutcome.Success).opportunities
        assertTrue(opportunities.isEmpty())
    }
}
