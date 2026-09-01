package com.ruleup.onboarding.presentation.onboarding

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.domain.entity.category.Category
import com.ruleup.onboarding.presentation.onboarding.viewmodel.OnboardingIntent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 02 · 관심. **건너뛸 수 있는 단계**다 — 하나도 안 고르고 넘어가도 가입이 끝나야 한다.
 * 여기서 잠그면 취향이 뚜렷하지 않은 사용자가 가입을 마치지 못한다.
 *
 * 제목·부제는 **일부러 단언하지 않는다.** Figma `1134:1725` 는 `어떤 습관에 관심 있나요?` /
 * `탐색 추천에 사용해요…` 인데 코드는 `어떤 챌린지에 관심 있나요?` /
 * `선택한 분야 기반으로 챌린지를 추천해드려요` 다. 어느 쪽이 맞는지는 기획 판단이라,
 * 여기서 한쪽을 못 박으면 그게 정답이 되어 버린다. TEST_STRATEGY.md 미검증 목록 참고.
 */
@RunWith(RobolectricTestRunner::class)
class InterestContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `하나도 고르지 않아도 다음 단계로 넘어간다`() {
        // 선택 단계라 잠그면 안 된다.
        val nav = compose.renderOnboarding { InterestContent(onIntent = {}, selected = emptyList()) }

        compose.onNodeWithText("다음").clickPastGuard()

        assertEquals(1, nav.pages.size)
    }

    @Test
    fun `고른 뒤에도 다음 단계로 넘어간다`() {
        val nav =
            compose.renderOnboarding {
                InterestContent(onIntent = {}, selected = listOf(Category.entries.first()))
            }

        compose.onNodeWithText("다음").clickPastGuard()

        assertEquals(1, nav.pages.size)
    }

    @Test
    fun `분야를 누르면 그 분야가 의도로 올라간다`() {
        val target = Category.entries.first()
        val intents = mutableListOf<OnboardingIntent>()
        compose.renderOnboarding { InterestContent(onIntent = { intents += it }, selected = emptyList()) }

        compose.onNodeWithText(target.label).clickPastGuard()

        assertTrue(intents.contains(OnboardingIntent.SetProfileInterest(target)))
    }
}
