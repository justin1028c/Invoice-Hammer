package com.fordham.toolbelt

import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.repository.InvoiceRepository
import com.fordham.toolbelt.domain.usecase.SaveInvoiceRequest
import com.fordham.toolbelt.domain.usecase.SaveInvoiceUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SaveInvoiceUseCaseTest {

    private val invoiceRepository: InvoiceRepository = mockk(relaxed = true)
    private lateinit var useCase: SaveInvoiceUseCase

    @Before
    fun setup() {
        useCase = SaveInvoiceUseCase(invoiceRepository)
    }

    private fun SaveInvoiceOutcome.unwrap(): Invoice {
        if (this is SaveInvoiceOutcome.Error) {
            fail("Expected Success but got Error: ${this.message}")
        }
        assertTrue("Expected Success but got $this", this is SaveInvoiceOutcome.Success)
        return (this as SaveInvoiceOutcome.Success).invoice
    }

    private fun request(
        clientName: String,
        clientAddress: String,
        subtotal: Double,
        taxRate: Double,
        depositAmount: Double,
        itemsSummary: String,
        pdfPath: String,
        isEstimate: Boolean,
        durationSeconds: Long = 0L
    ) = SaveInvoiceRequest(
        clientName = ClientName(clientName),
        clientAddress = ClientAddress(clientAddress),
        subtotal = MoneyAmount(subtotal),
        taxRate = TaxRatePercent(taxRate),
        depositAmount = MoneyAmount(depositAmount),
        itemsSummary = ItemsSummary(itemsSummary),
        pdfPath = PdfFilePath(pdfPath),
        isEstimate = isEstimate,
        durationSeconds = DurationSeconds(durationSeconds)
    )

    @Test
    fun `invoke creates invoice with correct total including tax`() = runTest {
        try {
            val invoiceSlot = slot<Invoice>()
            coEvery { invoiceRepository.insertInvoice(capture(invoiceSlot)) } returns InvoiceOutcome.Success

            val result = useCase(request(
                clientName = "John Smith",
                clientAddress = "123 Main St",
                subtotal = 1000.0,
                taxRate = 7.0,
                depositAmount = 200.0,
                itemsSummary = "Drywall, Painting",
                pdfPath = "/path/to/pdf.pdf",
                isEstimate = false
            ))

            val invoice = result.unwrap()

            assertEquals(870.0, invoice.totalAmount, 0.01)
            assertEquals("John Smith", invoice.clientName)
            assertEquals("123 Main St", invoice.clientAddress)
            assertEquals(200.0, invoice.depositAmount, 0.01)
            assertEquals("Drywall, Painting", invoice.itemsSummary)
            assertEquals("/path/to/pdf.pdf", invoice.pdfPath)
            assertFalse(invoice.isEstimate)
            assertFalse(invoice.isPaid)

            coVerify { invoiceRepository.insertInvoice(any()) }
            assertEquals(invoice.id, invoiceSlot.captured.id)
        } catch (t: Throwable) {
            t.printStackTrace()
            throw t
        }
    }

    @Test
    fun `invoke creates estimate when isEstimate is true`() = runTest {
        coEvery { invoiceRepository.insertInvoice(any()) } returns InvoiceOutcome.Success

        val invoice = useCase(request(
            clientName = "Jane Doe",
            clientAddress = "456 Oak Ave",
            subtotal = 500.0,
            taxRate = 0.0,
            depositAmount = 0.0,
            itemsSummary = "Roofing",
            pdfPath = "/path/to/estimate.pdf",
            isEstimate = true
        )).unwrap()

        assertTrue(invoice.isEstimate)
        assertEquals(500.0, invoice.totalAmount, 0.01)
    }

    @Test
    fun `invoke calculates zero tax correctly`() = runTest {
        coEvery { invoiceRepository.insertInvoice(any()) } returns InvoiceOutcome.Success

        val invoice = useCase(request(
            clientName = "Client",
            clientAddress = "Address",
            subtotal = 1000.0,
            taxRate = 0.0,
            depositAmount = 0.0,
            itemsSummary = "Labor",
            pdfPath = "",
            isEstimate = false
        )).unwrap()

        assertEquals(1000.0, invoice.totalAmount, 0.01)
    }

    @Test
    fun `invoke generates unique id`() = runTest {
        coEvery { invoiceRepository.insertInvoice(any()) } returns InvoiceOutcome.Success

        val invoice1 = useCase(request("A", "", 100.0, 0.0, 0.0, "", "", false)).unwrap()
        val invoice2 = useCase(request("B", "", 200.0, 0.0, 0.0, "", "", false)).unwrap()

        assertNotEquals(invoice1.id, invoice2.id)
    }

    @Test
    fun `invoke returns Error when repository throws`() = runTest {
        try {
            coEvery { invoiceRepository.insertInvoice(any()) } throws RuntimeException("DB failure")

            val result = useCase(request("Client", "Addr", 100.0, 7.0, 0.0, "Labor", "", false))

            assertTrue(result is SaveInvoiceOutcome.Error)
            val errorMsg = (result as SaveInvoiceOutcome.Error).message
            assertTrue(
                "Expected message to contain 'DB failure' or 'invoice', but was '$errorMsg'",
                errorMsg.contains("DB failure") || errorMsg.contains("invoice")
            )
        } catch (t: Throwable) {
            t.printStackTrace()
            throw t
        }
    }

    @Test
    fun `invoke sets isPaid false by default`() = runTest {
        coEvery { invoiceRepository.insertInvoice(any()) } returns InvoiceOutcome.Success

        val invoice = useCase(request("Client", "Addr", 500.0, 0.0, 0.0, "Labor", "", false)).unwrap()

        assertFalse(invoice.isPaid)
    }

    @Test
    fun `invoke persists durationSeconds`() = runTest {
        coEvery { invoiceRepository.insertInvoice(any()) } returns InvoiceOutcome.Success

        val invoice = useCase(request(
            clientName = "Client",
            clientAddress = "Addr",
            subtotal = 200.0,
            taxRate = 0.0,
            depositAmount = 0.0,
            itemsSummary = "Labor",
            pdfPath = "",
            isEstimate = false,
            durationSeconds = 3600L
        )).unwrap()

        assertEquals(3600L, invoice.durationSeconds)
    }
}
