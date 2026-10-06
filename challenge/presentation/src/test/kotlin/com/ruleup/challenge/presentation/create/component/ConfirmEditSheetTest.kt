package com.ruleup.challenge.presentation.create.component

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.challenge.presentation.create.viewmodel.CreateChallengeState
import com.ruleup.challenge.presentation.renderScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** 생성 확인 화면의 편집 시트. */
@RunWith(RobolectricTestRunner::class)
class ConfirmEditSheetTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `이름과 설명 시트에서 이름은 고칠 수 있고 설명은 보여 주기만 한다`() {
        // 생성 단계에서는 설명을 바꾸지 않는다(#580).
        compose.renderScreen {
            ConfirmEditSheet(
                section = ConfirmEditSection.TITLE_DESCRIPTION,
                state = CreateChallengeState.initial.copy(title = "아침 6시 기상", description = "매일 아침 6시에 일어나요"),
                onIntent = {},
                onDismiss = {},
            )
        }

        compose.onNode(hasText("아침 6시 기상") and hasSetTextAction()).assertExists()
        compose.onNodeWithText("매일 아침 6시에 일어나요").assertExists()
        compose.onNode(hasText("매일 아침 6시에 일어나요") and hasSetTextAction()).assertDoesNotExist()
    }
}
