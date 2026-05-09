package com.mustafanazeer.baselinems.battery.tap

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
class BilateralTapTestRenderTest {

    @get:Rule val composeRule = createComposeRule()

    @Test
    fun preInstructionsRender() {
        composeRule.setContent {
            BilateralTapTestContent(onComplete = {})
        }
        composeRule.onNodeWithText("Start dominant hand round").assertIsDisplayed()
    }
}
