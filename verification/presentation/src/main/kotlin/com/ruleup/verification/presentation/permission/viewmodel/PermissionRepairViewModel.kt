package com.ruleup.verification.presentation.permission.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.ui.mvi.NoEffect
import com.ruleup.verification.domain.repository.PermissionStatusProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 권한 재연결 ViewModel. */
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
                // 조회 실패 시 이전 상태 유지.
                runCatching { permissionStatusProvider.capture() }
                    .onSuccess { dispatch(PermissionRepairReducerEvent.Captured(it)) }
            }
        }
    }
