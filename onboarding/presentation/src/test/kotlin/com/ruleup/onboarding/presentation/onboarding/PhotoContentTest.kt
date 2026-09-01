package com.ruleup.onboarding.presentation.onboarding

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

/**
 * 05 · 사진. **건너뛸 수 있는 단계**다 — 사진이 없어도 닉네임 첫 글자 아바타로 시작한다.
 * 여기서 잠그면 사진이 없는 사용자가 가입을 마치지 못한다.
 *
 * 기대 문구 출처: Figma `1134:1832`「온보딩 5 · 사진」.
 */
@RunWith(RobolectricTestRunner::class)
class PhotoContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `어디에 보이는 사진인지 알린다`() {
        compose.renderOnboarding { PhotoContent(onIntent = {}) }

        compose.onNodeWithText("프로필 사진을 등록할까요?").assertExists()
        compose.onNodeWithText("그룹 멤버들에게 보여요").assertExists()
    }

    @Test
    fun `사진을 고르지 않아도 다음 단계로 넘어간다`() {
        // 선택 단계라 잠그면 안 된다.
        val nav = compose.renderOnboarding { PhotoContent(onIntent = {}, imageUri = null) }

        compose.onNodeWithText("다음").clickPastGuard()

        assertEquals(1, nav.pages.size)
    }

    @Test
    fun `사진을 골라도 다음 단계로 넘어간다`() {
        val nav = compose.renderOnboarding { PhotoContent(onIntent = {}, imageUri = "content://picked") }

        compose.onNodeWithText("다음").clickPastGuard()

        assertEquals(1, nav.pages.size)
    }
}
