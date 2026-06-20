package com.fordham.toolbelt.domain.usecase.agent

import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.model.agent.*
import com.fordham.toolbelt.domain.repository.InvoiceRepository
import com.fordham.toolbelt.domain.repository.JobNoteRepository
import com.fordham.toolbelt.domain.repository.ReceiptRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class GetProfitGuardianStatusUseCaseTest {

    private val invoiceRepository: InvoiceRepository = mockk()
    private val receiptRepository: ReceiptRepository = mockk()
    private val jobNoteRepository: JobNoteRepository = mockk()

    private val useCase = GetProfitGuardianStatusUseCase(
        invoiceRepository,
        receiptRepository,
        jobNoteRepository
    )

    private val invoiceId = InvoiceId("test-invoice-id")
    private val mockInvoice = Invoice(
        id = invoiceId,
        clientName = "John Doe",
        clientAddress = "123 Main St",
        date = "01/01/2026",
        totalAmount = 5000.0,
        itemsSummary = "Materials: 1000.0, Service: 4000.0",
        isPaid = false,
        isEstimate = true
    )

    @Test
    fun `returns Success status when actual materials matches budget`() = runTest {
        coEvery { invoiceRepository.getInvoiceById(invoiceId) } returns mockInvoice

        // Serialize a budget note where materials budget is 1000
        val lineItems = listOf(
            LineItem("Copper Pipe", 1000.0, "Materials")
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

        // Mock receipts containing materials totaling 1000
        val mockReceipts = listOf(
            ReceiptItem(
                id = ReceiptId("r-1"),
                description = "Home Depot Materials",
                quantity = 1.0,
                unitPrice = 1000.0,
                totalPrice = 1000.0,
                clientName = "John Doe",
                isBilled = true,
                linkedInvoiceId = invoiceId
            )
        )
        every { receiptRepository.allItems } returns flowOf(ReceiptListOutcome.Success(mockReceipts))

        val outcome = useCase(invoiceId)
        assertTrue(outcome is ProfitGuardianOutcome.Success)
        val status = (outcome as ProfitGuardianOutcome.Success).status
        assertEquals(1000.0, status.budgetedMaterials.value, 0.01)
        assertEquals(1000.0, status.actualMaterials.value, 0.01)
        assertEquals(0.0, status.materialVariance.value, 0.01)
        assertFalse(status.isTrendingNegative)
        assertTrue(status.reasons.isEmpty())
    }

    @Test
    fun `returns Success status with warning when actual materials exceed budget`() = runTest {
        coEvery { invoiceRepository.getInvoiceById(invoiceId) } returns mockInvoice

        val lineItems = listOf(
            LineItem("Copper Pipe", 1000.0, "Materials")
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

        // Actual materials = 1200 (20% overrun)
        val mockReceipts = listOf(
            ReceiptItem(
                id = ReceiptId("r-1"),
                description = "Home Depot Materials",
                quantity = 1.0,
                unitPrice = 1200.0,
                totalPrice = 1200.0,
                clientName = "John Doe",
                isBilled = true,
                linkedInvoiceId = invoiceId
            )
        )
        every { receiptRepository.allItems } returns flowOf(ReceiptListOutcome.Success(mockReceipts))

        val outcome = useCase(invoiceId)
        assertTrue(outcome is ProfitGuardianOutcome.Success)
        val status = (outcome as ProfitGuardianOutcome.Success).status
        assertEquals(1000.0, status.budgetedMaterials.value, 0.01)
        assertEquals(1200.0, status.actualMaterials.value, 0.01)
        assertEquals(200.0, status.materialVariance.value, 0.01)
        assertTrue(status.isTrendingNegative)
        assertTrue(status.reasons.isNotEmpty())
        assertTrue(status.reasons.any { it.value.contains("20.0%") })
    }

    @Test
    fun `returns ProjectNotFound when project does not exist`() = runTest {
        coEvery { invoiceRepository.getInvoiceById(invoiceId) } returns null

        val outcome = useCase(invoiceId)
        assertTrue(outcome is ProfitGuardianOutcome.ProjectNotFound)
        assertEquals(invoiceId, (outcome as ProfitGuardianOutcome.ProjectNotFound).invoiceId)
    }
}
