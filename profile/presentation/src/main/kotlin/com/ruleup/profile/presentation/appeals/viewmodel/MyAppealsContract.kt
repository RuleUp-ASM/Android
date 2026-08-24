package com.ruleup.profile.presentation.appeals.viewmodel

import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.NoEffect
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState
import com.ruleup.verification.domain.entity.AppealHistoryItem

sealed interface MyAppealsIntent : MviIntent {
    /** 화면 진입 시 이력 조회. */
    data object Load : MyAppealsIntent

    /** 전체 에러에서 다시 시도. */
    data object Retry : MyAppealsIntent

    data object Back : MyAppealsIntent
}

/**
 * 이의 내역 화면 상태.
 *
 * 잔여 횟수를 담지 않는다 — 이의 횟수 한도가 폐기됐다(챌린지 정책 §7.2). 남은 한도를 세어 보여주면
 * 있지도 않은 제약을 사용자에게 만들어 주게 된다.
 */
data class MyAppealsState(
    val isLoading: Boolean,
    val history: List<AppealHistoryItem>,
    val errorMessage: String?,
) : UiState {
    companion object {
        val initial =
            MyAppealsState(
                isLoading = true,
                history = emptyList(),
                errorMessage = null,
            )
    }
}

sealed interface MyAppealsReducerEvent : ReducerEvent {
    data object Loading : MyAppealsReducerEvent

    data class Loaded(
        val history: List<AppealHistoryItem>,
    ) : MyAppealsReducerEvent

    data class Failed(
        val message: String,
    ) : MyAppealsReducerEvent
}

/** 네비게이션은 NavigationHelper, 오류는 상태로 노출 — 단발성 이펙트 없음. */
typealias MyAppealsEffect = NoEffect
