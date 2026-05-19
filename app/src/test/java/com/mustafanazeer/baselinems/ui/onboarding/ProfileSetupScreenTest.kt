package com.mustafanazeer.baselinems.ui.onboarding

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mustafanazeer.baselinems.data.UserProfileDao
import com.mustafanazeer.baselinems.data.UserProfileEntity
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class ProfileSetupScreenTest {

    @get:Rule val composeRule = createComposeRule()

    private class FakeUserProfileDao : UserProfileDao {
        var insertedProfile: UserProfileEntity? = null
        override suspend fun insert(profile: UserProfileEntity) {
            insertedProfile = profile
        }
        override suspend fun getFirst(): UserProfileEntity? = insertedProfile
    }

    @Test
    fun saveButtonIsDisabledWhenYearAndHeightAreNonNumeric() {
        val dao = FakeUserProfileDao()
        var completed = false
        composeRule.setContent {
            ProfileSetupScreen(userProfileDao = dao, onComplete = { completed = true })
        }
        composeRule.onNodeWithText("Year of birth (for example 1985)").performTextInput("abcd")
        composeRule.onNodeWithText("Height in cm (for example 168)").performTextInput("xyz")
        composeRule.onNodeWithText("Save and continue").assertIsNotEnabled()
        composeRule.onNodeWithText("Save and continue").performClick()
        assertFalse("onComplete must not fire when inputs are invalid", completed)
    }

    @Test
    fun saveButtonClickWithNonNumericPasteDoesNotCrash() {
        val dao = FakeUserProfileDao()
        composeRule.setContent {
            ProfileSetupScreen(userProfileDao = dao, onComplete = {})
        }
        composeRule.onNodeWithText("Year of birth (for example 1985)").performTextInput("1990")
        composeRule.onNodeWithText("Save and continue").performClick()
    }
}
