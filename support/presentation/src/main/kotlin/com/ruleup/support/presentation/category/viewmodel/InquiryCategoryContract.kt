package com.ruleup.support.presentation.category.viewmodel

import com.ruleup.support.domain.entity.InquiryCategory
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.NoEffect
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface InquiryCategoryIntent : MviIntent {
    data object Back : InquiryCategoryIntent

    data class Select(
        val category: InquiryCategory,
    ) : InquiryCategoryIntent

    data object OpenHistory : InquiryCategoryIntent

    /** 「이런 건 더 빠른 길이 있어요」의 바로가기. 어디로 갈지는 [shortcut] 이 갖는다. */
    data class OpenShortcut(
        val shortcut: InquiryShortcut,
    ) : InquiryCategoryIntent
}

/**
 * 문의보다 빠른 경로 (Figma `1417:100`).
 *
 * 여기 있는 항목은 **이미 앱 안에 전용 화면이 있는 것들**이다. 문의로 받으면 영업일 2일이 걸리는데
 * 화면에서는 즉시 처리되므로, 접수 전에 먼저 보여 준다.
 */
enum class InquiryShortcut(
    val question: String,
    val actionLabel: String,
) {
    APPEAL("인증이 실패로 떴어요", "이의 제기"),
    WATCHING("감시자 알림을 끄고 싶어요", "패널티 수신 관리"),
}

/**
 * 이 화면은 서버를 부르지 않는다 — 분류 6종이 앱 상수라 로딩도 실패도 없다.
 *
 * 그래도 MVI 로 두는 건 화면이 `NavigationHelper` 를 직접 잡지 않게 하기 위해서다. Composable 이
 * 이동을 알면 렌더 검증에 네비게이션 가짜 객체가 따라 들어온다.
 */
data object InquiryCategoryState : UiState

/** 상태 전이가 없다 — reduce 가 받을 이벤트도 없다. */
sealed interface InquiryCategoryReducerEvent : ReducerEvent

typealias InquiryCategoryEffect = NoEffect
