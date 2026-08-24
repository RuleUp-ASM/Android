package com.ruleup.verification.presentation.permission.viewmodel

import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.NoEffect
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState
import com.ruleup.verification.domain.entity.PermissionSnapshot

sealed interface PermissionRepairIntent : MviIntent {
    /** 화면 진입·설정 복귀 시 권한 재조회. 저장하지 않고 매번 OS 에 다시 묻는다. */
    data object Refresh : PermissionRepairIntent

    data object Back : PermissionRepairIntent
}

data class PermissionRepairState(
    val permissions: PermissionSnapshot? = null,
) : UiState {
    companion object {
        val initial = PermissionRepairState()
    }
}

sealed interface PermissionRepairReducerEvent : ReducerEvent {
    data class Captured(
        val permissions: PermissionSnapshot,
    ) : PermissionRepairReducerEvent
}

/** 네비게이션은 NavigationHelper — 단발성 이펙트 없음. */
typealias PermissionRepairEffect = NoEffect
