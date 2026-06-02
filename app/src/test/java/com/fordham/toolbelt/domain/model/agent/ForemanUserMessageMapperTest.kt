package com.fordham.toolbelt.domain.model.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ForemanUserMessageMapperTest {
    @Test
    fun `single quick invoice step speaks client name`() {
        val message = ForemanUserMessageMapper.forStep(
            ChainedToolStep(
                toolCallId = ToolCallId("t1"),
                toolName = ToolName.QuickInvoice,
                result = ToolExecutionResult.QuickInvoiceCompleted(
                    invoiceId = com.fordham.toolbelt.domain.model.InvoiceId("inv-1"),
                    clientName = NaturalLanguage("Bob"),
                    totalAmount = 200.0
                )
            )
        )

        assertEquals("Invoice saved for Bob.", message.spoken.value)
        assertTrue(message.stepLabels.isEmpty())
    }

    @Test
    fun `multi-step chain exposes overlay labels`() {
        val steps = listOf(
            ChainedToolStep(
                toolCallId = ToolCallId("t1"),
                toolName = ToolName.SearchClients,
                result = ToolExecutionResult.ClientSearchCompleted(
                    listOf(ClientSearchHit(com.fordham.toolbelt.domain.model.ClientId("c1"), NaturalLanguage("Bob")))
                )
            ),
            ChainedToolStep(
                toolCallId = ToolCallId("t2"),
                toolName = ToolName.QuickInvoice,
                result = ToolExecutionResult.QuickInvoiceCompleted(
                    invoiceId = com.fordham.toolbelt.domain.model.InvoiceId("inv-1"),
                    clientName = NaturalLanguage("Bob"),
                    totalAmount = 200.0
                )
            )
        )

        val message = ForemanUserMessageMapper.forChain(steps, NaturalLanguage("All set."))

        assertEquals("All set.", message.spoken.value)
        assertEquals(2, message.stepLabels.size)
        assertTrue(message.stepLabels.first().contains("Searched clients"))
    }
}
