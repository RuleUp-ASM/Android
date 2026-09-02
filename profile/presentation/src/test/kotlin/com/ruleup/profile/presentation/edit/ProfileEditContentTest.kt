package com.ruleup.profile.presentation.edit

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.domain.entity.category.Category
import com.ruleup.profile.domain.entity.Profile
import com.ruleup.profile.presentation.edit.viewmodel.ProfileEditIntent
import com.ruleup.profile.presentation.edit.viewmodel.ProfileEditState
import com.ruleup.profile.presentation.renderScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 프로필 편집. 저장은 되돌리기 어려운 동작이라 **저장 중에 다시 눌리면 안 된다** — 연타하면
 * 수정 요청이 두 번 나가고 그 사이 서버 버전이 어긋난다.
 */
@RunWith(RobolectricTestRunner::class)
class ProfileEditContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `불러오는 중에는 실패 문구를 띄우지 않는다`() {
        render(ProfileEditState.initial.copy(isLoading = true))

        compose.onNodeWithText("프로필을 불러오지 못했어요").assertDoesNotExist()
    }

    @Test
    fun `조회에 실패하면 사유를 보여 준다`() {
        render(ProfileEditState.initial.copy(isLoading = false, errorMessage = "권한이 없어요"))

        compose.onNodeWithText("권한이 없어요").assertExists()
    }

    @Test
    fun `사유를 모르는 실패도 빈 화면으로 두지 않는다`() {
        render(ProfileEditState.initial.copy(isLoading = false, errorMessage = null))

        compose.onNodeWithText("프로필을 불러오지 못했어요").assertExists()
    }

    @Test
    fun `저장 중이면 진행 중임을 문구로 알린다`() {
        // 연타 방지 자체는 ViewModel 이 한다(ProfileEditViewModelTest). 화면은 진행 중이라는
        // 사실을 알리는 몫이라, 여기서는 문구가 바뀌는 것만 본다 — 층을 건너뛰어 다시 세지 않는다.
        render(loaded().copy(isSaving = true))

        compose.onNodeWithText("저장 중…").assertExists()
        compose.onNodeWithText("저장").assertDoesNotExist()
    }

    @Test
    fun `저장 중이 아니면 저장 버튼을 보여 준다`() {
        render(loaded())

        compose.onNodeWithText("저장").assertExists()
    }

    @Test
    fun `관심 분야가 어디에 쓰이는지 알린다`() {
        // 왜 고르는지 모르면 사용자가 아무거나 고르고, 추천 품질이 떨어진다.
        render(loaded())

        compose.onNodeWithText("관심 분야는 추천 챌린지와 알림에 사용돼요").assertExists()
    }

    private fun loaded() =
        ProfileEditState.initial.copy(
            isLoading = false,
            profile = profile(),
            nickname = "지현",
            selectedCategories = listOf(Category.entries.first()),
        )

    private fun profile() =
        Profile(
            id = "u1",
            nickname = "지현",
            email = null,
            profileImageUrl = null,
            nicknameChangedAt = null,
            nicknameChangeableAfter = null,
            mannerTemperature = 36.5,
            interestCategories = listOf(Category.entries.first()),
            createdAt = "2026-01-01T00:00:00Z",
        )

    private fun render(
        state: ProfileEditState,
        onIntent: (ProfileEditIntent) -> Unit = {},
    ) {
        compose.renderScreen { ProfileEditContent(state = state, onIntent = onIntent, onPickImage = {}) }
    }
}
