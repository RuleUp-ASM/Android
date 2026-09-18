package com.ruleup.onboarding.presentation.walkthrough.viewmodel

import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

/**
 * 워크쓰루 3장. 순서·개수와 **문구까지** 여기가 단일 소스다 — 화면이 장마다 문구를 들고 있으면
 * 장을 옮길 때 점 표시와 내용이 어긋난다(Figma `1460:2`·`1461:2`·`1462:2`).
 */
enum class WalkthroughPageIndex(
    val eyebrow: String,
    val title: String,
    val description: String,
) {
    CREATE(
        eyebrow = "01 · 만들기",
        title = "한 문장이면\n챌린지가 완성돼요",
        description = "하고 싶은 루틴을 적으면 AI가 인증 방법·목표·기간까지 초안을 만들어 줘요.",
    ),
    KEEP(
        eyebrow = "02 · 지키기",
        title = "인증은 스마트폰이\n알아서 해요",
        description = "위치·걸음·앱 사용 시간·기상 신호로 자동 판정해요. 사진을 찍어 올릴 필요가 없어요.",
    ),
    STAY(
        eyebrow = "03 · 남기기",
        title = "꾸준함이\n티어로 남아요",
        description = "인증 결과가 점수로 쌓여 브론즈부터 루비까지 올라가요. 챌린지가 끝나도 기록은 사라지지 않아요.",
    ),
    ;

    val isLast: Boolean get() = this == entries.last()

    fun next(): WalkthroughPageIndex = entries.getOrElse(ordinal + 1) { this }
}

sealed interface WalkthroughIntent : MviIntent {
    /** CTA. 마지막 장이면 끝내고 로그인으로 간다. */
    data object Next : WalkthroughIntent

    /** 상단 건너뛰기. 마지막 장에는 없다 — 거기서는 CTA 가 곧 끝내기다. */
    data object Skip : WalkthroughIntent
}

data class WalkthroughState(
    val page: WalkthroughPageIndex,
) : UiState {
    companion object {
        val initial = WalkthroughState(page = WalkthroughPageIndex.CREATE)
    }
}

sealed interface WalkthroughReducerEvent : ReducerEvent {
    data class PageChanged(
        val page: WalkthroughPageIndex,
    ) : WalkthroughReducerEvent
}
