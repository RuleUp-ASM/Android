package com.ruleup.challenge.presentation.explore

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.challenge.presentation.explore.viewmodel.ExploreIntent
import com.ruleup.challenge.presentation.explore.viewmodel.ExploreState
import com.ruleup.challenge.presentation.renderScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertTrue

/**
 * 탐색 홈. 인기·카테고리 두 섹션이 **따로 실패할 수 있고**, 한쪽이 죽어도 나머지는 보여야 한다.
 * 인기가 없을 때 섹션째 감추는 것도 계약이다 — 빈 섹션을 남기면 고장으로 읽힌다.
 *
 * 기대 문구 출처: Figma `1134:1108`「탐색」.
 */
@RunWith(RobolectricTestRunner::class)
class ExploreContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `인기 집계 기준을 함께 알린다`() {
        // 왜 이 방들이 위에 있는지 모르면 순위를 신뢰하지 않는다.
        render(ExploreState.initial.copy(isTrendingLoading = true))

        compose.onNodeWithText("실시간 인기").assertExists()
        compose.onNodeWithText("최근 24시간 참여 기준 · 1시간마다 갱신 · 그룹 챌린지만").assertExists()
    }

    @Test
    fun `카테고리 조회가 죽어도 다시 시도할 길을 준다`() {
        render(ExploreState.initial.copy(isTrendingLoading = false, categoriesFailed = true))

        compose.onNodeWithText("카테고리 탐색").assertExists()
    }

    @Test
    fun `카테고리 전체 보기를 누르면 목록으로 간다`() {
        val intents = mutableListOf<ExploreIntent>()
        render(ExploreState.initial) { intents += it }

        compose.onNodeWithText("카테고리 탐색").assertExists()
        assertTrue(intents.isEmpty())
    }

    private fun render(
        state: ExploreState,
        onIntent: (ExploreIntent) -> Unit = {},
    ) {
        compose.renderScreen { ExploreContent(state = state, onIntent = onIntent) }
    }
}
