package com.mustafanazeer.baselinems.battery.tap

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class BilateralTapTestRestingTest {

    @get:Rule val composeRule = createComposeRule()

    @Test
    fun interRoundRestSecondsConstantIs5() {
        assertEquals(5, INTER_ROUND_REST_SECONDS)
    }

    @Test
    fun restingBetweenRoundsRendersInitialCountdownAndCopy() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            BilateralTapTestContent(
                onComplete = {},
                initialPhase = TapPhase.RestingBetweenRounds(secondsRemaining = INTER_ROUND_REST_SECONDS)
            )
        }
        composeRule.mainClock.advanceTimeBy(50L)
        composeRule.onNodeWithText("5").assertIsDisplayed()
        composeRule.onNodeWithText("Rest for a moment, then we will start the other hand.").assertIsDisplayed()
    }

    @Test
    fun restingCountdownTicksDownAndTransitionsToCountdownOnTick5() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            BilateralTapTestContent(
                onComplete = {},
                initialPhase = TapPhase.RestingBetweenRounds(secondsRemaining = INTER_ROUND_REST_SECONDS)
            )
        }
        composeRule.mainClock.advanceTimeBy(50L)
        composeRule.onNodeWithText("5").assertIsDisplayed()

        composeRule.mainClock.advanceTimeBy(1_000L)
        composeRule.onNodeWithText("4").assertIsDisplayed()

        composeRule.mainClock.advanceTimeBy(1_000L)
        composeRule.onNodeWithText("3").assertIsDisplayed()

        composeRule.mainClock.advanceTimeBy(1_000L)
        composeRule.onNodeWithText("2").assertIsDisplayed()

        composeRule.mainClock.advanceTimeBy(1_000L)
        composeRule.onNodeWithText("1").assertIsDisplayed()

        composeRule.mainClock.advanceTimeBy(1_000L)
        composeRule.onNodeWithText("Non dominant hand").assertIsDisplayed()
    }
}
