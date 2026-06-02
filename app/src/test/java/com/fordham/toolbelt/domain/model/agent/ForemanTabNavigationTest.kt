package com.fordham.toolbelt.domain.model.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ForemanTabNavigationTest {
    @Test
    fun `parses bottom bar tab phrases`() {
        assertEquals(AppTab.NewInvoice, ForemanTabNavigation.parseNavigationOnly("open new tab"))
        assertEquals(AppTab.NewInvoice, ForemanTabNavigation.parseNavigationOnly("open new invoice tab"))
        assertEquals(AppTab.NewInvoice, ForemanTabNavigation.parseNavigationOnly("open the new invoice"))
        assertEquals(AppTab.NewInvoice, ForemanTabNavigation.parseNavigationOnly("go to new invoice"))
        assertEquals(AppTab.Suppliers, ForemanTabNavigation.parseNavigationOnly("go to stores"))
        assertEquals(AppTab.History, ForemanTabNavigation.parseNavigationOnly("show past"))
        assertEquals(AppTab.Receipts, ForemanTabNavigation.parseNavigationOnly("receipts"))
    }

    @Test
    fun `rejects commands that include invoice work`() {
        assertNull(ForemanTabNavigation.parseNavigationOnly("open new tab and bill john"))
        assertNull(ForemanTabNavigation.parseNavigationOnly("find acme"))
        assertNull(ForemanTabNavigation.parseNavigationOnly("bill john for labor"))
    }

    @Test
    fun `normalizes voice fillers and homophones`() {
        assertEquals(AppTab.NewInvoice, ForemanTabNavigation.parseNavigationOnly("hey foreman open knew invoice tab"))
        assertEquals(AppTab.Clients, ForemanTabNavigation.parseNavigationOnly("go to customers"))
        assertEquals(AppTab.Stats, ForemanTabNavigation.parseNavigationOnly("show dashboard"))
    }

    @Test
    fun `parses expanded bottom bar synonyms`() {
        assertEquals(AppTab.History, ForemanTabNavigation.parseNavigationOnly("show jobs"))
        assertEquals(AppTab.Receipts, ForemanTabNavigation.parseNavigationOnly("expenses"))
        assertEquals(AppTab.Suppliers, ForemanTabNavigation.parseNavigationOnly("materials"))
        assertEquals(AppTab.Settings, ForemanTabNavigation.parseNavigationOnly("profile"))
        assertEquals(AppTab.NewInvoice, ForemanTabNavigation.parseNavigationOnly("editor"))
    }
}
