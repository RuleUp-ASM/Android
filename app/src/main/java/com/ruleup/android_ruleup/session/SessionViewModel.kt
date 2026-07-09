package com.ruleup.android_ruleup.session

import androidx.lifecycle.ViewModel
import com.ruleup.onboarding.domain.auth.usecase.ObserveSessionEndUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 앱 전역 세션 상태를 관찰한다. 토큰 만료(Authenticator 의 정리)나 명시적 로그아웃으로
 * 세션이 로그인→로그아웃으로 전이할 때 [sessionEnded] 로 알린다.
 */
@HiltViewModel
class SessionViewModel
    @Inject
    constructor(
        observeSessionEndUseCase: ObserveSessionEndUseCase,
    ) : ViewModel() {
        /**
         * 로그인 상태였다가 로그아웃으로 바뀌는 전이에서만 방출한다.
         * 최초 방출(앱 시작 시점의 로그인 여부)은 건너뛰어, Splash 의 초기 라우팅과 충돌하지 않게 한다.
         */
        val sessionEnded: Flow<Unit> = observeSessionEndUseCase()
    }
