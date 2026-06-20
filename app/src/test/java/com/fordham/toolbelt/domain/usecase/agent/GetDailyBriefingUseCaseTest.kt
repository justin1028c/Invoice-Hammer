package com.fordham.toolbelt.domain.usecase.agent

import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.model.agent.*
import com.fordham.toolbelt.domain.repository.InvoiceRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class GetDailyBriefingUseCaseTest {

    private val invoiceRepository: InvoiceRepository = mockk()
    private val getProfitGuardianStatus: GetProfitGuardianStatusUseCase = mockk()
    private val detectChangeOrders: DetectChangeOrdersUseCase = mockk()

    private val useCase = GetDailyBriefingUseCase(
        invoiceRepository,
        getProfitGuardianStatus,
        detectChangeOrders
    )

    @Test
    fun `compiles briefing with overdue invoices, budget overruns, and change order opportunities`() = runTest {
        val unpaidInvoice = Invoice(
            id = InvoiceId("unpaid-inv"),
            clientName = "Overdue Client",
            clientAddress = "",
            date = "06/01/2026",
            totalAmount = 3000.0,
            itemsSummary = "Drywall",
            isPaid = false,
            isEstimate = false,
            lastUpdated = System.currentTimeMillis()
        )
        val activeEstimate = Invoice(
            id = InvoiceId("active-est"),
            clientName = "Active Client",
            clientAddress = "",
            date = "06/10/2026",
            totalAmount = 5000.0,
            itemsSummary = "Roofing",
            isPaid = false,
            isEstimate = true,
            lastUpdated = System.currentTimeMillis()
        )

        every { invoiceRepository.allInvoices } returns flowOf(listOf(unpaidInvoice, activeEstimate))

        // Mock profit guardian overrun for active estimate
        val mockProfitStatus = ProfitGuardianStatus(
            invoiceId = activeEstimate.id,
            clientName = ClientName("Active Client"),
            budgetedRevenue = MoneyAmount(5000.0),
            projectedRevenue = MoneyAmount(5000.0),
            budgetedMaterials = MoneyAmount(1000.0),
            actualMaterials = MoneyAmount(1500.0), // $500 overrun
            materialVariance = MoneyVariance(500.0),
            projectedProfit = MoneyAmount(4000.0),
            currentProjection = MoneyAmount(3500.0), // trending negative
            reasons = listOf(NaturalLanguage("Material overrun")),
            recommendations = listOf(NaturalLanguage("Mitigate"))
        )
        coEvery { getProfitGuardianStatus(activeEstimate.id) } returns ProfitGuardianOutcome.Success(mockProfitStatus)

        // Mock high confidence change order opportunity for active estimate
        val mockChangeOrderOpportunity = ChangeOrderOpportunity(
            invoiceId = activeEstimate.id,
            clientName = ClientName("Active Client"),
            detectedTask = NaturalLanguage("Extra electrical outlet"),
            recommendedItems = listOf(LineItem("Outlet", 200.0, "Service")),
            estimatedValueRange = 200.0..250.0,
            confidence = OpportunityConfidence.VERY_HIGH
        )
        coEvery { detectChangeOrders(activeEstimate.id) } returns DetectChangeOrdersOutcome.Success(listOf(mockChangeOrderOpportunity))

        val outcome = useCase.execute()
        assertTrue(outcome is GetDailyBriefingOutcome.Success)
        val briefing = (outcome as GetDailyBriefingOutcome.Success).briefing

        assertEquals(1, briefing.overdueInvoiceCount)
        assertEquals(3000.0, briefing.totalOverdueAmount.value, 0.01)
        assertEquals(1, briefing.budgetOverruns.size)
        assertEquals(1, briefing.unbilledOpportunities.size)

        // Total recovery: opportunity (200.0) + overrun (500.0) = 700.0
        assertEquals(700.0, briefing.potentialProfitRecovery.value, 0.01)

        // Overdue invoice reminder is $3000, change order is $200, budget warning is $500.
        // Highest impact recommended action should be sending the overdue reminder.
        assertNotNull(briefing.primaryAction)
        assertEquals("Send invoice reminder to Overdue Client", briefing.primaryAction!!.title.value)
        assertEquals(3000.0, briefing.primaryAction!!.estimatedImpact.value, 0.01)
    }
}
