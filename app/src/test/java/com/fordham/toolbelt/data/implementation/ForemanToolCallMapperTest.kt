package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.domain.model.ForemanToolCall
import com.fordham.toolbelt.domain.model.InvoiceId
import com.fordham.toolbelt.domain.model.ToolParameters
import com.fordham.toolbelt.domain.model.ToolType
import com.fordham.toolbelt.domain.model.agent.AgentOutcome
import com.fordham.toolbelt.domain.model.agent.CreateChangeOrderArgs
import com.fordham.toolbelt.domain.model.agent.DetectChangeOrdersArgs
import com.fordham.toolbelt.domain.model.agent.GetDailyBriefingArgs
import com.fordham.toolbelt.domain.model.agent.GetProfitGuardianStatusArgs
import com.fordham.toolbelt.domain.model.agent.NaturalLanguage
import com.fordham.toolbelt.domain.model.agent.ToolName
import com.fordham.toolbelt.domain.repository.ClientRepository
import com.fordham.toolbelt.domain.repository.InvoiceRepository
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ForemanToolCallMapperTest {
    private val clientRepository: ClientRepository = mockk()
    private val invoiceRepository: InvoiceRepository = mockk()
    private val mapper = ForemanToolCallMapper(clientRepository, invoiceRepository)

    @Test
    fun testMapGetProfitGuardianStatus() = runTest {
        val toolCall = ForemanToolCall(
            id = "call-1",
            type = ToolType.GET_PROFIT_GUARDIAN_STATUS,
            parameters = ToolParameters.GetProfitGuardianStatus("invoice-123")
        )
        val outcome = mapper.toAgentOutcome(toolCall)
        assertTrue(outcome is AgentOutcome.ToolExecutionRequested)
        val requested = outcome as AgentOutcome.ToolExecutionRequested
        assertEquals(ToolName.GetProfitGuardianStatus, requested.toolName)
        val args = requested.arguments as GetProfitGuardianStatusArgs
        assertEquals(InvoiceId("invoice-123"), args.invoiceId)
    }

    @Test
    fun testMapDetectChangeOrders() = runTest {
        val toolCall = ForemanToolCall(
            id = "call-2",
            type = ToolType.DETECT_CHANGE_ORDERS,
            parameters = ToolParameters.DetectChangeOrders("invoice-456")
        )
        val outcome = mapper.toAgentOutcome(toolCall)
        assertTrue(outcome is AgentOutcome.ToolExecutionRequested)
        val requested = outcome as AgentOutcome.ToolExecutionRequested
        assertEquals(ToolName.DetectChangeOrders, requested.toolName)
        val args = requested.arguments as DetectChangeOrdersArgs
        assertEquals(InvoiceId("invoice-456"), args.invoiceId)
    }

    @Test
    fun testMapGetDailyBriefing() = runTest {
        val toolCall = ForemanToolCall(
            id = "call-3",
            type = ToolType.GET_DAILY_BRIEFING,
            parameters = ToolParameters.GetDailyBriefing
        )
        val outcome = mapper.toAgentOutcome(toolCall)
        assertTrue(outcome is AgentOutcome.ToolExecutionRequested)
        val requested = outcome as AgentOutcome.ToolExecutionRequested
        assertEquals(ToolName.GetDailyBriefing, requested.toolName)
        val args = requested.arguments as GetDailyBriefingArgs
        assertTrue(args.timestamp > 0L)
    }

    @Test
    fun testMapCreateChangeOrder() = runTest {
        val toolCall = ForemanToolCall(
            id = "call-4",
            type = ToolType.CREATE_CHANGE_ORDER,
            parameters = ToolParameters.CreateChangeOrder(
                invoiceId = "invoice-789",
                description = "Extra outlets requested",
                amount = 250.0
            )
        )
        val outcome = mapper.toAgentOutcome(toolCall)
        assertTrue(outcome is AgentOutcome.ToolExecutionRequested)
        val requested = outcome as AgentOutcome.ToolExecutionRequested
        assertEquals(ToolName.CreateChangeOrder, requested.toolName)
        val args = requested.arguments as CreateChangeOrderArgs
        assertEquals(InvoiceId("invoice-789"), args.invoiceId)
        assertEquals(NaturalLanguage("Extra outlets requested"), args.description)
        assertEquals(250.0, args.amount, 0.0)
    }

    @Test
    fun testMapNone() = runTest {
        val toolCall = ForemanToolCall(
            id = "call-5",
            type = ToolType.UNKNOWN,
            parameters = ToolParameters.None
        )
        val outcome = mapper.toAgentOutcome(toolCall)
        assertTrue(outcome is AgentOutcome.TextResponse)
        val textResponse = outcome as AgentOutcome.TextResponse
        assertEquals(NaturalLanguage("No action required."), textResponse.response)
    }
}
