package com.ruleup.domain.navigation

/** 단일 네비게이션 플로우에 흘려보내는 신호. */
sealed interface NavSignal {
    data class GoToDestPage(
        val route: NavRoute,
    ) : NavSignal

    /** 백스택을 [route] 의 시작 스택으로 교체한다. */
    data class ReplaceStack(
        val route: NavRoute,
    ) : NavSignal

    data object Back : NavSignal
}
