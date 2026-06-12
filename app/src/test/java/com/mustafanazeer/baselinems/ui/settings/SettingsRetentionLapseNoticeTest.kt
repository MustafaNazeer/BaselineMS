package com.mustafanazeer.baselinems.ui.settings

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.mustafanazeer.baselinems.battery.voice.VoiceSettings
import com.mustafanazeer.baselinems.data.UserProfileDao
import com.mustafanazeer.baselinems.data.UserProfileEntity
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SettingsRetentionLapseNoticeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val noticeFragment = "no longer has a lock screen"

    private class FakeUserProfileDao : UserProfileDao {
        override suspend fun insert(profile: UserProfileEntity) = Unit
        override suspend fun getFirst(): UserProfileEntity? = null
    }

    @Test
    fun `advisory renders when a retention lapse is pending`() {
        VoiceSettings.markRetentionLapsed(context)

        composeRule.setContent {
            SettingsScreen(userProfileDao = FakeUserProfileDao(), deviceSecureProvider = { false })
        }

        composeRule.onNodeWithText(noticeFragment, substring = true).assertIsDisplayed()
    }

    @Test
    fun `advisory is absent when no lapse is pending`() {
        composeRule.setContent {
            SettingsScreen(userProfileDao = FakeUserProfileDao(), deviceSecureProvider = { true })
        }

        composeRule.onNodeWithText(noticeFragment, substring = true).assertDoesNotExist()
    }

    @Test
    fun `displaying the advisory clears the pending flag so it shows only once`() {
        VoiceSettings.markRetentionLapsed(context)

        composeRule.setContent {
            SettingsScreen(userProfileDao = FakeUserProfileDao(), deviceSecureProvider = { false })
        }
        composeRule.waitForIdle()

        assertFalse(VoiceSettings.retentionLapsedNoticePending(context))
    }
}
