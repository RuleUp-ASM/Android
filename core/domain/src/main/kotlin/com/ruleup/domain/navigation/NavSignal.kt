package com.ruleup.domain.navigation

/** 단일 네비게이션 플로우에 흘려보내는 신호. */
sealed interface NavSignal {
    data class GoToDestPage(
        val route: NavRoute,
    ) : NavSignal

    /**
     * 백스택을 [route] 의 시작 스택으로 교체한다.
     *
     * 스플래시가 인증을 마치고 딥링크 목적지로 보낼 때 쓴다. 전진 이동([GoToDestPage])으로
     * 처리하면 스플래시 위에 목적지가 쌓여, 뒤로 갔을 때 스플래시가 다시 판정을 돌리는
     * 순환에 빠진다.
     */
    data class ReplaceStack(
        val route: NavRoute,
    ) : NavSignal

    data object Back : NavSignal
}
