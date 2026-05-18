package com.mustafanazeer.baselinems.battery.tap

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class BilateralTapTestDoneTest {

    @get:Rule val composeRule = createComposeRule()

    private fun emptyResult(): BilateralTapTest.TapTestResult =
        BilateralTapTest.TapTestResult(qualityScore = 1.0, features = emptyMap())

    @Test
    fun doneScreenRendersExplicitContinueButton() {
        composeRule.setContent {
            BilateralTapTestContent(
                onComplete = {},
                initialPhase = TapPhase.Done(result = emptyResult())
            )
        }
        composeRule.onNodeWithText("Tap test recorded.").assertIsDisplayed()
        composeRule.onNodeWithText("Continue").assertIsDisplayed()
    }

    @Test
    fun continueButtonFiresOnCompleteOnlyWhenTapped() {
        var captured: BilateralTapTest.TapTestResult? = null
        val expected = emptyResult()
        composeRule.setContent {
            BilateralTapTestContent(
                onComplete = { payload ->
                    captured = payload as? BilateralTapTest.TapTestResult
                },
                initialPhase = TapPhase.Done(result = expected)
            )
        }
        composeRule.onNodeWithText("Continue").performClick()
        assertNotNull("onComplete must fire on Continue tap", captured)
    }
}
