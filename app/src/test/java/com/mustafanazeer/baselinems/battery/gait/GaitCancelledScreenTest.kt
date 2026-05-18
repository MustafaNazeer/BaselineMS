package com.mustafanazeer.baselinems.battery.gait

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class GaitCancelledScreenTest {

    @get:Rule val composeRule = createComposeRule()

    @Test
    fun cancelledScreenRendersNothingWasSavedAndBackToHomeCta() {
        composeRule.setContent {
            GaitCancelledScreen(onDone = {})
        }
        composeRule.onNodeWithText("Test cancelled").assertIsDisplayed()
        composeRule.onNodeWithText("Test cancelled. Nothing was saved.").assertIsDisplayed()
        composeRule.onNodeWithText("Back to home").assertIsDisplayed()
    }
}
