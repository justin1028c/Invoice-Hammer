package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.model.subscription.SubscriptionFeature
import com.fordham.toolbelt.domain.repository.GeminiRepository
import com.fordham.toolbelt.domain.usecase.subscription.HasSubscriptionFeatureUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ComposeAiInvoiceReminderUseCaseTest {

    private val geminiRepository: GeminiRepository = mockk()
    private val hasSubscriptionFeature: HasSubscriptionFeatureUseCase = mockk()
    private lateinit var useCase: ComposeAiInvoiceReminderUseCase

    private val testInvoice = Invoice(
        id = InvoiceId("INV-123"),
        clientName = "Alice Smith",
        clientAddress = "123 Maple St",
        clientPhone = PhoneNumber("555-0199"),
        clientEmail = EmailAddress("alice@example.com"),
        date = "2026-06-17",
        totalAmount = 1500.0,
        depositAmount = 500.0,
        itemsSummary = "Kitchen Tile Installation",
        isPaid = false,
        isEstimate = false
    )

    @Before
    fun setup() {
        useCase = ComposeAiInvoiceReminderUseCase(geminiRepository, hasSubscriptionFeature)
    }

    @Test
    fun `invoke returns Failure when subscription does not cover AiAgent`() = runTest {
        every { hasSubscriptionFeature(SubscriptionFeature.AiAgent) } returns false

        val outcome = useCase(
            invoice = testInvoice,
            tone = ReminderTone.FRIENDLY,
            channel = ReminderChannel.EMAIL,
            contractorName = "Bob the Builder",
            paymentLink = "https://pay.example.com"
        )

        assertTrue(outcome is GeminiOutcome.Failure)
        assertEquals(
            "Pro subscription required for AI reminder generation.",
            (outcome as GeminiOutcome.Failure).error.value
        )
    }

    @Test
    fun `invoke constructs prompt with friendly email parameters and processes task`() = runTest {
        every { hasSubscriptionFeature(SubscriptionFeature.AiAgent) } returns true

        val promptSlot = slot<String>()
        coEvery {
            geminiRepository.processTask(TaskType.GENERATE, capture(promptSlot))
        } returns GeminiOutcome.Success("{\"subject\": \"Friendly Reminder\", \"body\": \"Hello Alice...\"}")

        val outcome = useCase(
            invoice = testInvoice,
            tone = ReminderTone.FRIENDLY,
            channel = ReminderChannel.EMAIL,
            contractorName = "Bob the Builder",
            paymentLink = "https://pay.example.com"
        )

        assertTrue(outcome is GeminiOutcome.Success)
        val textResult = (outcome as GeminiOutcome.Success).text
        assertTrue(textResult.contains("Friendly Reminder"))

        val capturedPrompt = promptSlot.captured
        assertTrue(capturedPrompt.contains("Alice Smith"))
        assertTrue(capturedPrompt.contains("INV-123"))
        assertTrue(capturedPrompt.contains("1500.0"))
        assertTrue(capturedPrompt.contains("500.0"))
        assertTrue(capturedPrompt.contains("Kitchen Tile Installation"))
        assertTrue(capturedPrompt.contains("Bob the Builder"))
        assertTrue(capturedPrompt.contains("https://pay.example.com"))
        assertTrue(capturedPrompt.contains("polite, friendly, and helpful"))
        assertTrue(capturedPrompt.contains("Optimize this for Email"))

        coVerify { geminiRepository.processTask(TaskType.GENERATE, any()) }
    }

    @Test
    fun `invoke constructs prompt with firm sms parameters and processes task`() = runTest {
        every { hasSubscriptionFeature(SubscriptionFeature.AiAgent) } returns true

        val promptSlot = slot<String>()
        coEvery {
            geminiRepository.processTask(TaskType.GENERATE, capture(promptSlot))
        } returns GeminiOutcome.Success("{\"subject\": \"Reminder\", \"body\": \"Your payment is overdue...\"}")

        val outcome = useCase(
            invoice = testInvoice,
            tone = ReminderTone.FIRM,
            channel = ReminderChannel.SMS,
            contractorName = "Bob",
            paymentLink = "https://pay.example.com"
        )

        assertTrue(outcome is GeminiOutcome.Success)
        val capturedPrompt = promptSlot.captured
        assertTrue(capturedPrompt.contains("firm and serious"))
        assertTrue(capturedPrompt.contains("Optimize this for SMS"))

        coVerify { geminiRepository.processTask(TaskType.GENERATE, any()) }
    }
}
