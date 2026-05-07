package com.mustafan4x.msbattery.battery.tap

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class TwoTargetsOffTargetTest {

    @get:Rule val composeRule = createComposeRule()

    @Test
    fun inTargetTap_doesNotIncrementOffTargetCount() {
        var inTargetCount = 0
        var offTargetCount = 0

        composeRule.setContent {
            TwoTargets(
                onInTargetTap = { inTargetCount += 1 },
                onOffTargetTap = { offTargetCount += 1 }
            )
        }

        composeRule.onNodeWithText("L").performClick()
        assertEquals("in-target count after left tap", 1, inTargetCount)
        assertEquals("off-target count after left tap", 0, offTargetCount)

        composeRule.onNodeWithText("R").performClick()
        assertEquals("in-target count after right tap", 2, inTargetCount)
        assertEquals("off-target count after right tap", 0, offTargetCount)
    }

    @Test
    fun offTargetTap_doesNotIncrementInTargetCount() {
        var inTargetCount = 0
        var offTargetCount = 0

        composeRule.setContent {
            TwoTargets(
                onInTargetTap = { inTargetCount += 1 },
                onOffTargetTap = { offTargetCount += 1 }
            )
        }

        composeRule.onNodeWithTag(TWO_TARGETS_TAG).performTouchInput {
            click(Offset(center.x, 2f))
        }
        assertEquals("in-target count after gap tap", 0, inTargetCount)
        assertEquals("off-target count after gap tap", 1, offTargetCount)
    }
}
