package com.ruleup.onboarding.presentation.intro.screen

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruleup.designsystem.theme.RuleUpTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReviewLoginDialogDeviceTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun emptyCodeCannotBeSubmittedAndEnteredCodeIsReturned() {
        var submitted: String? = null
        compose.setContent { RuleUpTheme { ReviewLoginDialog(onDismiss = {}, onSubmit = { submitted = it }) } }

        compose.onNodeWithText("로그인").assertIsNotEnabled()
        assertNull(submitted)
        compose.onNodeWithText("심사 코드").performTextInput("REVIEW-123")
        compose.onNodeWithText("로그인").assertIsEnabled().performClick()

        assertEquals("REVIEW-123", submitted)
    }

    @Test
    fun cancelDismissesWithoutSubmittingCode() {
        var dismissed = false
        var submitted: String? = null
        compose.setContent { RuleUpTheme { ReviewLoginDialog(onDismiss = { dismissed = true }, onSubmit = { submitted = it }) } }

        compose.onNodeWithText("심사 코드").performTextInput("REVIEW-123")
        compose.onNodeWithText("취소").performClick()

        assertTrue(dismissed)
        assertNull(submitted)
    }

    @Test
    fun submissionInProgressCannotBeDismissedOrSubmittedAgain() {
        var dismissed = false
        var submitted = false
        compose.setContent {
            RuleUpTheme {
                ReviewLoginDialog(onDismiss = { dismissed = true }, onSubmit = { submitted = true }, isSubmitting = true)
            }
        }

        compose.onNodeWithText("로그인").assertIsNotEnabled()
        compose.onNodeWithText("취소").assertIsNotEnabled()
        assertTrue(!dismissed && !submitted)
    }
}
