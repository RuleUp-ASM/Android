package com.ruleup.challenge.presentation.explore.list

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.challenge.domain.entity.ExploreFilter
import com.ruleup.challenge.domain.entity.ExploreSort
import com.ruleup.challenge.presentation.clickPastGuard
import com.ruleup.challenge.presentation.explore.list.viewmodel.ExploreListIntent
import com.ruleup.challenge.presentation.explore.list.viewmodel.ExploreListState
import com.ruleup.challenge.presentation.renderScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertTrue

/** 둘러보기 목록. */
@RunWith(RobolectricTestRunner::class)
class ExploreListContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `티어 조건 때문에 비었으면 조건을 끌 수 있게 해 준다`() {
        render(state(filter = ExploreFilter(eligibleOnly = true)))

        compose.onNodeWithText("내 티어로 들어갈 수 있는 챌린지가 없어요").assertExists()
        compose.onNodeWithText("티어 조건 끄기").assertExists()
    }

    @Test
    fun `기록이 부족해 비었으면 조건 탓으로 돌리지 않는다`() {
        // 지표 정렬은 표본 미달 방을 아예 빼므로 조건을 풀어도 안 나온다.
        render(state(sort = ExploreSort.COMPLETION_RATE))

        compose.onNodeWithText("아직 기록이 충분한 챌린지가 없어요").assertExists()
        compose.onNodeWithText("티어 조건 끄기").assertDoesNotExist()
    }

    @Test
    fun `카테고리에 방이 없으면 조건을 좁혔다고 하지 않는다`() {
        render(state())

        compose.onNodeWithText("이 카테고리에는 아직 챌린지가 없어요").assertExists()
    }

    @Test
    fun `필터 때문에 비었으면 조건이 좁다고 알린다`() {
        render(
            state(
                filter =
                    ExploreFilter(
                        categories =
                            setOf(
                                com.ruleup.domain.entity.category.Category.entries
                                    .first(),
                            ),
                    ),
            ),
        )

        compose.onNodeWithText("조건에 맞는 챌린지가 없어요").assertExists()
    }

    @Test
    fun `아직 불러오는 중이면 비었다고 하지 않는다`() {
        render(state().copy(isLoading = true))

        compose.onNodeWithText("이 카테고리에는 아직 챌린지가 없어요").assertDoesNotExist()
    }

    @Test
    fun `티어 조건 끄기를 누르면 그 의도가 올라간다`() {
        val intents = mutableListOf<ExploreListIntent>()
        render(state(filter = ExploreFilter(eligibleOnly = true))) { intents += it }

        compose.onNodeWithText("티어 조건 끄기").clickPastGuard()

        assertTrue(intents.contains(ExploreListIntent.ClearEligibleOnly))
    }

    private fun state(
        filter: ExploreFilter = ExploreFilter.none,
        sort: ExploreSort = ExploreSort.default,
    ) = ExploreListState.initial.copy(isLoading = false, filter = filter, sort = sort, items = emptyList())

    private fun render(
        state: ExploreListState,
        onIntent: (ExploreListIntent) -> Unit = {},
    ) {
        compose.renderScreen { ExploreListContent(state = state, onIntent = onIntent) }
    }
}
