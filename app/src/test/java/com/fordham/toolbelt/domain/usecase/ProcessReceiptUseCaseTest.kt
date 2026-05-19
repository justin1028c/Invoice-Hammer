package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.repository.GeminiRepository
import com.fordham.toolbelt.domain.repository.ReceiptRepository
import com.fordham.toolbelt.domain.repository.SettingsRepository
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ProcessReceiptUseCaseTest {

    private val geminiRepository: GeminiRepository = mockk()
    private val receiptRepository: ReceiptRepository = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk()
    private lateinit var useCase: ProcessReceiptUseCase

    private val testImageBytes = byteArrayOf(1, 2, 3, 4, 5)
    private val testItems = listOf(
        ReceiptItem(id = com.fordham.toolbelt.domain.model.ReceiptId("item1"), description = "Lumber", quantity = 1.0, unitPrice = 50.0, totalPrice = 50.0),
        ReceiptItem(id = com.fordham.toolbelt.domain.model.ReceiptId("item2"), description = "Nails", quantity = 2.0, unitPrice = 10.0, totalPrice = 20.0)
    )

    @Before
    fun setup() {
        useCase = ProcessReceiptUseCase(geminiRepository, receiptRepository, settingsRepository)
    }

    private fun request(clientName: String? = null) = ProcessReceiptRequest(
        imageBytes = testImageBytes,
        clientName = clientName
    )

    @Test
    fun `invoke saves items with client name when premium is active`() = runTest {
        coEvery { settingsRepository.businessSettingsFlow } returns flowOf(BusinessSettings(isPremium = true))
        coEvery { geminiRepository.processReceiptImage(testImageBytes) } returns ReceiptImageOutcome.Success(testItems)
        val itemsSlot = slot<List<ReceiptItem>>()
        coEvery { receiptRepository.insertItems(capture(itemsSlot)) } returns ReceiptOutcome.Success

        val result = useCase(request("John Smith"))

        assertTrue(result is ProcessReceiptOutcome.Success)
        val savedItems = itemsSlot.captured
        assertEquals(2, savedItems.size)
        assertEquals("John Smith", savedItems[0].clientName)
        assertEquals("John Smith", savedItems[1].clientName)
    }

    @Test
    fun `invoke returns PremiumRequired when user is not premium`() = runTest {
        coEvery { settingsRepository.businessSettingsFlow } returns flowOf(BusinessSettings(isPremium = false))

        val result = useCase(request("Client"))

        assertTrue(result is ProcessReceiptOutcome.PremiumRequired)
        coVerify(exactly = 0) { geminiRepository.processReceiptImage(any()) }
        coVerify(exactly = 0) { receiptRepository.insertItems(any()) }
    }

    @Test
    fun `invoke returns error when AI fails`() = runTest {
        coEvery { settingsRepository.businessSettingsFlow } returns flowOf(BusinessSettings(isPremium = true))
        coEvery { geminiRepository.processReceiptImage(testImageBytes) } returns ReceiptImageOutcome.Failure(
            FailureMessage("AI processing failed")
        )

        val result = useCase(request("Client"))

        assertTrue(result is ProcessReceiptOutcome.Failure)
        val error = result as ProcessReceiptOutcome.Failure
        assertEquals("AI processing failed", error.error.value)
    }

    @Test
    fun `invoke returns error when exception thrown`() = runTest {
        coEvery { settingsRepository.businessSettingsFlow } returns flowOf(BusinessSettings(isPremium = true))
        coEvery { geminiRepository.processReceiptImage(testImageBytes) } throws RuntimeException("Unexpected error")

        val result = useCase(request("Client"))

        assertTrue(result is ProcessReceiptOutcome.Failure)
    }
}
