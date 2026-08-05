package com.ruleup.android_ruleup.session

import androidx.lifecycle.ViewModel
import com.ruleup.observability.domain.api.Observability
import com.ruleup.observability.domain.event.Channel
import com.ruleup.onboarding.domain.auth.usecase.ObserveSessionEndUseCase
import com.ruleup.onboarding.domain.observability.OnboardingEvents
import com.ruleup.onboarding.domain.observability.SessionExpiredTrigger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
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
        private val observability: Observability,
    ) : ViewModel() {
        /**
         * 로그인 상태였다가 로그아웃으로 바뀌는 전이에서만 방출한다.
         * 최초 방출(앱 시작 시점의 로그인 여부)은 건너뛰어, Splash 의 초기 라우팅과 충돌하지 않게 한다.
         */
        val sessionEnded: Flow<Unit> =
            observeSessionEndUseCase().onEach {
                // 단일 활성 기기 정책의 부작용을 모니터링한다. 여기서는 만료와 다른 기기 로그인을
                // 구분할 수 없다 — 둘 다 토큰 정리로 끝나고 사유는 서버 응답에만 있다.
                observability.log(Channel.BUSINESS) {
                    OnboardingEvents.sessionExpired(SessionExpiredTrigger.EXPIRED)
                }
            }
    }
