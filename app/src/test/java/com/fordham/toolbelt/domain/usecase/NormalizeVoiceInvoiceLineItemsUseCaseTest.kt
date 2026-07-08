package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.AiInvoiceResult
import com.fordham.toolbelt.domain.model.ItemsSummary
import com.fordham.toolbelt.domain.model.LineItem
import com.fordham.toolbelt.domain.model.MoneyAmount
import org.junit.Assert.assertEquals
import org.junit.Test

class NormalizeVoiceInvoiceLineItemsUseCaseTest {
    private val useCase = NormalizeVoiceInvoiceLineItemsUseCase()

    @Test
    fun `expands short valid trade descriptions before voice guardrails`() {
        val result = useCase(
            AiInvoiceResult(
                items = listOf(
                    LineItem(ItemsSummary("Labor"), MoneyAmount(200.0), "Labor"),
                    LineItem(ItemsSummary("Service"), MoneyAmount(100.0), "Service"),
                    LineItem(ItemsSummary("Materials"), MoneyAmount(80.0), "Materials")
                )
            )
        )

        assertEquals("Labor service", result.items[0].description.value)
        assertEquals("General service", result.items[1].description.value)
        assertEquals("Materials and supplies", result.items[2].description.value)
    }

    @Test
    fun `leaves specific descriptions unchanged`() {
        val result = useCase(
            AiInvoiceResult(
                items = listOf(
                    LineItem(
                        description = ItemsSummary("Installed 2 ceiling fans"),
                        amount = MoneyAmount(300.0),
                        category = "Electrical"
                    )
                )
            )
        )

        assertEquals("Installed 2 ceiling fans", result.items.single().description.value)
    }
}
