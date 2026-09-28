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

/** 02 · 관심. */
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
