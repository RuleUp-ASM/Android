package com.ruleup.verification.presentation.permission.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.ui.mvi.NoEffect
import com.ruleup.verification.domain.repository.PermissionStatusProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 권한 재연결 ViewModel (프론트엔드 테크스펙 4-1「권한 · 재연결」).
 *
 * 권한 상태를 **저장하지 않는다.** 사용자가 설정에서 켜고 돌아오는 것이 이 화면의 주된 동선이라
 * 캐시가 곧 거짓이 된다 — 화면에 들어올 때마다, 설정에서 돌아올 때마다 OS 에 다시 묻는다.
 */
@HiltViewModel
class PermissionRepairViewModel
    @Inject
    constructor(
        private val permissionStatusProvider: PermissionStatusProvider,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<PermissionRepairIntent, PermissionRepairState, PermissionRepairReducerEvent, NoEffect>(
            PermissionRepairState.initial,
        ) {
        override fun onIntent(intent: PermissionRepairIntent) {
            when (intent) {
                PermissionRepairIntent.Refresh -> refresh()
                PermissionRepairIntent.Back -> navigationHelper.navigateToBack()
            }
        }

        override fun reduce(
            state: PermissionRepairState,
            event: PermissionRepairReducerEvent,
        ): PermissionRepairState =
            when (event) {
                is PermissionRepairReducerEvent.Captured -> state.copy(permissions = event.permissions)
            }

        private fun refresh() {
            viewModelScope.launch {
                // 조회에 실패하면 직전 값을 유지한다 — 모른다고 "다 끊겼다"로 그리면 없던 사고가 된다.
                runCatching { permissionStatusProvider.capture() }
                    .onSuccess { dispatch(PermissionRepairReducerEvent.Captured(it)) }
            }
        }
    }
