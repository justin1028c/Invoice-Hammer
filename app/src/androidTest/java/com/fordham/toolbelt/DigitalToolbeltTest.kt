package com.fordham.toolbelt

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class DigitalToolbeltTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun verifyRebrandingAndTabs() {
        // 1. Verify the AppBar Title
        composeTestRule.onNodeWithText("DIGITAL TOOLBELT").assertIsDisplayed()

        // 2. Verify all tabs are present in the ScrollableTabRow
        composeTestRule.onNodeWithText("NEW").assertIsDisplayed()
        composeTestRule.onNodeWithText("PAST").assertIsDisplayed()
        composeTestRule.onNodeWithText("COSTS").assertIsDisplayed()
        composeTestRule.onNodeWithText("STATS").assertIsDisplayed()
        composeTestRule.onNodeWithText("JOBS").assertIsDisplayed()
    }

    @Test
    fun testJobTimerWorkflow() {
        // 1. Check if Timer is visible and starts at 0
        composeTestRule.onNodeWithText("JOB TIMER").assertIsDisplayed()
        composeTestRule.onNodeWithText("00:00:00").assertIsDisplayed()

        // 2. Click START
        composeTestRule.onNodeWithText("START").performClick()
        
        // 3. Verify STOP button appears
        composeTestRule.onNodeWithText("STOP").assertIsDisplayed()
        
        // 4. Click STOP
        composeTestRule.onNodeWithText("STOP").performClick()
        
        // 5. Verify "BILL TIME" UI appears
        composeTestRule.onNodeWithText("BILL TIME").assertIsDisplayed()
        composeTestRule.onNodeWithText("Rate ($/hr)").assertIsDisplayed()
    }

    @Test
    fun testNavigationToStatsAndJobs() {
        // Navigate to STATS
        composeTestRule.onNodeWithText("STATS").performClick()
        composeTestRule.onNodeWithText("BUSINESS HEALTH").assertIsDisplayed()
        composeTestRule.onNodeWithText("CSV").assertIsDisplayed()
        composeTestRule.onNodeWithText("ZIP BUNDLE").assertIsDisplayed()

        // Navigate to JOBS
        composeTestRule.onNodeWithText("JOBS").performClick()
        composeTestRule.onNodeWithText("CLIENT DIRECTORY").assertIsDisplayed()
        composeTestRule.onNodeWithText("ADD NEW SERVICE").assertIsDisplayed()
    }
}
