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

    /** 「이런 건 더 빠른 길이 있어요」의 바로가기. */
    data class OpenShortcut(
        val shortcut: InquiryShortcut,
    ) : InquiryCategoryIntent
}

/** 문의보다 빠른 경로. */
enum class InquiryShortcut(
    val question: String,
    val actionLabel: String,
) {
    APPEAL("인증이 실패로 떴어요", "이의 제기"),
    WATCHING("감시자 알림을 끄고 싶어요", "패널티 수신 관리"),
}

/** 이 화면은 서버를 부르지 않는다 */
data object InquiryCategoryState : UiState

/** 상태 전이가 없다 */
sealed interface InquiryCategoryReducerEvent : ReducerEvent

typealias InquiryCategoryEffect = NoEffect
