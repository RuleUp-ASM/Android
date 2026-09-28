package com.ruleup.challenge.domain.navigation

import com.ruleup.challenge.domain.entity.ExploreSort
import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.Page

/** 탐색 메인 페이지(실시간 인기 + 카테고리 그리드). */
object ChallengeExplorePage : Page {
    const val PATH = AppRoutes.CHALLENGE_EXPLORE

    override fun toRoute(): NavRoute = NavRoute(PATH)
}

/** 챌린지 둘러보기(목록) 페이지. */
data class ChallengeExploreListPage(
    val category: Category? = null,
    val sort: ExploreSort? = null,
) : Page {
    override fun toRoute(): NavRoute =
        NavRoute(
            PATH,
            buildMap {
                category?.let { put(ARG_CATEGORY, it.value) }
                sort?.let { put(ARG_SORT, it.value) }
            },
        )

    companion object {
        const val PATH = AppRoutes.CHALLENGE_EXPLORE_LIST
        const val ARG_CATEGORY = "category"
        const val ARG_SORT = "sort"
    }
}
