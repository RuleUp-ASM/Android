package com.ruleup.onboarding.domain.auth

import com.ruleup.domain.entity.user.TermsVersions
import com.ruleup.onboarding.domain.auth.usecase.AutoLoginUseCase
import com.ruleup.onboarding.domain.auth.usecase.BackfillUserIdUseCase
import com.ruleup.onboarding.domain.intro.usecase.LoadIntroUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/** [SessionBootstrap] 의 진행 상태. */
sealed interface SessionBootstrapState {
    /** 아직 판정 전. 화면은 대기 상태를 보여준다. */
    data object Running : SessionBootstrapState

    /** 강제 업데이트 — 더 진행하지 않는다. */
    data class ForceUpdate(
        val message: String?,
    ) : SessionBootstrapState

    /** 판정 완료. [authenticated] 가 false 면 로그인이 필요하다. */
    data class Resolved(
        val authenticated: Boolean,
    ) : SessionBootstrapState
}

/**
 * 앱 진입 시 **한 번만** 도는 버전 게이트 + 자동 로그인.
 *
 * 액티비티 `onCreate` 에서 [start] 를 호출해 컴포지션과 겹쳐 돌린다. 자동 로그인은 DataStore
 * 읽기에 더해 토큰 재발급 **네트워크 호출**을 포함하므로 동기로 끝낼 수 없다 — 그래서 결과를
 * 기다리는 화면(스플래시)은 여전히 필요하고, 여기서 줄어드는 건 그 대기 시간이다.
 *
 * [start] 는 멱등이다. 액티비티 재생성으로 여러 번 불려도 판정은 한 번만 돌고, 이미 나온
 * 결과는 [state] 로 그대로 다시 관측된다.
 */
@Singleton
class SessionBootstrap
    @Inject
    constructor(
        private val loadIntroUseCase: LoadIntroUseCase,
        private val autoLoginUseCase: AutoLoginUseCase,
        private val backfillUserIdUseCase: BackfillUserIdUseCase,
    ) {
        private val _state = MutableStateFlow<SessionBootstrapState>(SessionBootstrapState.Running)
        val state: StateFlow<SessionBootstrapState> = _state.asStateFlow()

        /**
         * 현행 약관 버전. 가입 화면이 동의 기록에 실어 보낸다.
         *
         * 인트로 응답에 딸려 오는데 정작 쓰는 건 온보딩 마지막 단계라, 여기서 받아 두고 그때 꺼낸다.
         * 인트로 호출이 실패하면(페일오픈) null 이고, 가입 화면은 폴백 버전으로 진행한다.
         */
        @Volatile
        var termsVersions: TermsVersions? = null
            private set

        private val started = AtomicBoolean(false)
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun start() {
            if (!started.compareAndSet(false, true)) return
            scope.launch {
                // 버전 게이트 먼저. 페일오픈(null)이면 정상 흐름을 그대로 진행한다.
                val intro = loadIntroUseCase()
                termsVersions = intro?.termsVersions
                val gate = intro?.versionGate
                if (gate?.forceUpdate == true) {
                    _state.value = SessionBootstrapState.ForceUpdate(gate.devTestMsg)
                    return@launch
                }
                val authenticated = autoLoginUseCase()
                // 판정을 먼저 방출한다 — 백필을 기다리면 모든 콜드스타트에 왕복 한 번이 얹힌다.
                _state.value = SessionBootstrapState.Resolved(authenticated)
                if (authenticated) backfillUserIdUseCase()
            }
        }
    }
