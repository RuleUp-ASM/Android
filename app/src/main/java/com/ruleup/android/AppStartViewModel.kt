package com.ruleup.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruleup.domain.auth.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * 앱 cold start 시 저장된 세션을 보고 시작 화면을 결정한다.
 * 결정 전에는 [AppStartState.Loading] 으로 splash 를 유지한다.
 */
@HiltViewModel
class AppStartViewModel
    @Inject
    constructor(
        sessionRepository: SessionRepository,
    ) : ViewModel() {
        val startState: StateFlow<AppStartState> =
            sessionRepository.isLoggedIn
                .map { loggedIn -> AppStartState.Resolved(isLoggedIn = loggedIn) }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = AppStartState.Loading,
                )

        /** 토큰 갱신 실패로 세션이 강제 종료됐을 때 발행된다. 네비게이션을 로그인으로 리셋하는 데 쓴다. */
        val sessionExpired: Flow<Unit> = sessionRepository.sessionExpired
    }

sealed interface AppStartState {
    data object Loading : AppStartState

    data class Resolved(
        val isLoggedIn: Boolean,
    ) : AppStartState
}
