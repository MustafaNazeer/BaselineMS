package com.mustafanazeer.baselinems.ui.reports

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AboutCitationsScreenRenderTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun renders_top_app_bar_title() {
        composeRule.setContent {
            AboutCitationsScreen(onBack = {})
        }
        composeRule.onNodeWithText("About these charts").assertIsDisplayed()
    }

    @Test
    fun renders_about_the_trends_section() {
        composeRule.setContent {
            AboutCitationsScreen(onBack = {})
        }
        composeRule.onNodeWithText("About the trends").assertIsDisplayed()
        composeRule.onNodeWithText(
            "The Trends screens show your own values over time. They are personal to you. This app does not compare your values against clinical norms or population averages in this version; trends here are personal trends only."
        ).assertIsDisplayed()
    }

    @Test
    fun renders_about_the_quality_bands_section() {
        composeRule.setContent {
            AboutCitationsScreen(onBack = {})
        }
        composeRule.onNodeWithText("About the quality bands").assertIsDisplayed()
        composeRule.onNodeWithText(
            "The quality band on each session (Reliable, Mostly reliable, Less reliable) reflects how cleanly the recording was captured, not how well you did. Less reliable sessions still count; longer trends matter more than single sessions."
        ).assertIsDisplayed()
    }

    @Test
    fun renders_about_data_privacy_section() {
        composeRule.setContent {
            AboutCitationsScreen(onBack = {})
        }
        composeRule.onNodeWithText("About data privacy").assertIsDisplayed()
        composeRule.onNodeWithText(
            "All of your data stays on this device. Nothing is sent to a cloud or to anyone else unless you choose to export a report and share it yourself."
        ).assertIsDisplayed()
    }

    @Test
    fun renders_disclaimer_section_verbatim_from_spec_section_10() {
        composeRule.setContent {
            AboutCitationsScreen(onBack = {})
        }
        composeRule.onNodeWithText("Disclaimer").assertIsDisplayed()
        composeRule.onNodeWithText(
            "This is not a medical device. It does not diagnose or treat any condition. Do not change your treatment based on these results. Share with your neurologist for clinical decisions."
        ).assertIsDisplayed()
    }

    @Test
    fun renders_back_arrow_navigation_icon() {
        composeRule.setContent {
            AboutCitationsScreen(onBack = {})
        }
        composeRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }

    @Test
    fun back_arrow_fires_on_back_callback() {
        var fired = false
        composeRule.setContent {
            AboutCitationsScreen(onBack = { fired = true })
        }
        composeRule.onNodeWithContentDescription("Back").performClick()
        assertTrue(fired)
    }

    @Test
    fun does_not_render_givon_2009_citation_block_in_v1() {
        composeRule.setContent {
            AboutCitationsScreen(onBack = {})
        }
        composeRule.onNodeWithText("Givon 2009", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("Healthy adult average", substring = true).assertDoesNotExist()
    }
}
