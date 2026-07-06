package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.AiInvoiceResult
import com.fordham.toolbelt.domain.model.DraftInvoice
import com.fordham.toolbelt.domain.model.ItemsSummary
import com.fordham.toolbelt.domain.model.LineItem
import com.fordham.toolbelt.domain.model.MoneyAmount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildVoiceInvoiceApplicationPlanUseCaseTest {
    private val useCase = BuildVoiceInvoiceApplicationPlanUseCase()

    @Test
    fun `builds apply plan for confident invoice result`() {
        val lineItem = LineItem(
            description = ItemsSummary("Drywall repair"),
            amount = MoneyAmount(450.0),
            category = "Service"
        )

        val plan = useCase(
            draft = DraftInvoice(),
            ai = AiInvoiceResult(
                clientName = "John Smith",
                clientAddress = "12 Main Street",
                items = listOf(lineItem),
                depositAmount = 100.0,
                taxRatePercent = 8.0,
                laborRate = 75.0,
                confidenceScore = 0.95
            )
        )

        assertEquals("John Smith", plan.clientName)
        assertEquals("12 Main Street", plan.clientAddress)
        assertEquals(8.0, plan.taxRatePercent!!, 0.01)
        assertEquals(100.0, plan.depositAmount!!, 0.01)
        assertEquals(75.0, plan.hourlyRate!!, 0.01)
        assertEquals(listOf(lineItem), plan.pendingLineItems)
        assertFalse(plan.requiresFollowUp)
    }

    @Test
    fun `blocks low confidence result from applying draft fields`() {
        val plan = useCase(
            draft = DraftInvoice(clientName = "Existing", clientAddress = "Existing Address"),
            ai = AiInvoiceResult(
                clientName = "Wrong Client",
                clientAddress = "Wrong Address",
                items = listOf(
                    LineItem(
                        description = ItemsSummary("Unclear work"),
                        amount = MoneyAmount(250.0)
                    )
                ),
                depositAmount = 100.0,
                taxRatePercent = 10.0,
                laborRate = 80.0,
                confidenceScore = 0.4,
                validationIssues = listOf("LOW_AUDIO_CONFIDENCE")
            )
        )

        assertNull(plan.clientName)
        assertNull(plan.clientAddress)
        assertNull(plan.taxRatePercent)
        assertNull(plan.depositAmount)
        assertNull(plan.hourlyRate)
        assertTrue(plan.pendingLineItems.isEmpty())
        assertTrue(plan.requiresFollowUp)
        assertTrue(plan.validationIssues.contains("LOW_AUDIO_CONFIDENCE"))
    }

    @Test
    fun `requires follow up when client is missing and draft has no client`() {
        val plan = useCase(
            draft = DraftInvoice(),
            ai = AiInvoiceResult(
                items = listOf(
                    LineItem(
                        description = ItemsSummary("Painting"),
                        amount = MoneyAmount(600.0)
                    )
                ),
                confidenceScore = 0.9,
                validationIssues = listOf("MISSING_CLIENT_NAME")
            )
        )

        assertNull(plan.clientName)
        assertTrue(plan.pendingLineItems.isEmpty())
        assertTrue(plan.requiresFollowUp)
    }

    @Test
    fun `keeps line items pending when address is missing but client is known`() {
        val lineItem = LineItem(
            description = ItemsSummary("Fixture install"),
            amount = MoneyAmount(300.0)
        )

        val plan = useCase(
            draft = DraftInvoice(clientAddress = ""),
            ai = AiInvoiceResult(
                clientName = "Acme",
                items = listOf(lineItem),
                confidenceScore = 0.9,
                validationIssues = listOf("MISSING_CLIENT_ADDRESS")
            )
        )

        assertEquals("Acme", plan.clientName)
        assertNull(plan.clientAddress)
        assertEquals(listOf(lineItem), plan.pendingLineItems)
        assertFalse(plan.requiresFollowUp)
        assertTrue(plan.validationIssues.contains("MISSING_CLIENT_ADDRESS"))
    }
}
