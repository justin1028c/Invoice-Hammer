package com.fordham.toolbelt.domain.model.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ForemanCommandRouterTest {
    @Test
    fun `routes tab navigation locally without LLM`() {
        val route = ForemanCommandRouter.route("hey foreman please open new invoice tab")
        assertTrue(route is ForemanRoute.LocalTab)
        assertEquals(AppTab.NewInvoice, (route as ForemanRoute.LocalTab).tab)
    }

    @Test
    fun `normalizes STT homophones for tab nav`() {
        val route = ForemanCommandRouter.route("open knew invoice tab")
        assertTrue(route is ForemanRoute.LocalTab)
        assertEquals(AppTab.NewInvoice, (route as ForemanRoute.LocalTab).tab)
    }

    @Test
    fun `routes quick invoice macro locally`() {
        val route = ForemanCommandRouter.route("invoice Bob 200 for labor")
        assertTrue(route is ForemanRoute.LocalMacro)
        val macro = route as ForemanRoute.LocalMacro
        assertEquals(ToolName.QuickInvoice, macro.toolName)
        val args = macro.arguments as QuickInvoiceArgs
        assertEquals("Bob", args.clientName.value)
        assertEquals(1, args.lineItems.size)
        assertEquals(200.0, args.lineItems.first().amount, 0.01)
    }

    @Test
    fun `routes hours at rate macro locally`() {
        val route = ForemanCommandRouter.route("bill John 3 hours at 85")
        assertTrue(route is ForemanRoute.LocalMacro)
        val macro = route as ForemanRoute.LocalMacro
        assertEquals(ToolName.QuickInvoice, macro.toolName)
        val args = macro.arguments as QuickInvoiceArgs
        assertEquals("John", args.clientName.value)
        assertEquals(3.0, args.lineItems.first().quantity!!, 0.01)
        assertEquals(85.0, args.lineItems.first().unitPrice!!, 0.01)
    }

    @Test
    fun `routes unbilled receipts macro locally`() {
        val route = ForemanCommandRouter.route("bill unbilled for Acme Roofing")
        assertTrue(route is ForemanRoute.LocalMacro)
        val macro = route as ForemanRoute.LocalMacro
        assertEquals(ToolName.QuickInvoiceFromUnbilledReceipts, macro.toolName)
        val args = macro.arguments as QuickInvoiceFromUnbilledReceiptsArgs
        assertEquals("Acme Roofing", args.clientName.value)
    }

    @Test
    fun `routes duplicate last invoice macro locally`() {
        val route = ForemanCommandRouter.route("duplicate last invoice for Smith")
        assertTrue(route is ForemanRoute.LocalMacro)
        val macro = route as ForemanRoute.LocalMacro
        assertEquals(ToolName.DuplicateLastInvoice, macro.toolName)
    }

    @Test
    fun `routes same as last time macro locally`() {
        val route = ForemanCommandRouter.route("same as last time for Mike")
        assertTrue(route is ForemanRoute.LocalMacro)
        assertEquals(ToolName.DuplicateLastInvoice, (route as ForemanRoute.LocalMacro).toolName)
    }

    @Test
    fun `routes duplicate and edit macro locally`() {
        val route = ForemanCommandRouter.route("same as last time for Mike and add 50 for materials")
        assertTrue(route is ForemanRoute.LocalMacro)
        val macro = route as ForemanRoute.LocalMacro
        assertEquals(ToolName.DuplicateAndEdit, macro.toolName)
        val args = macro.arguments as DuplicateAndEditArgs
        assertEquals("Mike", args.clientName.value)
        assertEquals(1, args.additionalLineItems.size)
    }

    @Test
    fun `routes new client quick invoice macro to LLM chain`() {
        val route = ForemanCommandRouter.route("new client Bob invoice 200 for labor")
        assertTrue(route is ForemanRoute.LlmChain)
    }

    @Test
    fun `compound work falls through to LLM chain`() {
        val route = ForemanCommandRouter.route("open new tab and bill john")
        assertTrue(route is ForemanRoute.LlmChain)
    }

    @Test
    fun `ambiguous create invoice uses LLM chain`() {
        val route = ForemanCommandRouter.route("create a new invoice")
        assertTrue(route is ForemanRoute.LlmChain)
    }
}
