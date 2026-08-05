package com.ruleup.onboarding.domain.auth

import com.ruleup.domain.entity.user.TermsVersions
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.PendingDeepLink
import com.ruleup.domain.navigation.RouteAccessPolicy
import com.ruleup.domain.token.TokenRepository
import com.ruleup.observability.domain.api.Observability
import com.ruleup.observability.domain.api.w
import com.ruleup.onboarding.domain.auth.usecase.AutoLoginUseCase
import com.ruleup.onboarding.domain.intro.usecase.LoadIntroUseCase
import com.ruleup.onboarding.domain.navigation.HomePage
import com.ruleup.onboarding.domain.navigation.LoginPage
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

private const val TAG = "[AppEntry]"

/** [SessionBootstrap] 의 진행 상태. */
sealed interface SessionBootstrapState {
    /** 아직 판정 전. 화면은 대기 상태를 보여준다. */
    data object Running : SessionBootstrapState

    /**
     * 강제 업데이트 — 더 진행하지 않는다.
     *
     * @property minAppVersion 안내 문구에 넣을 최소 버전. 없으면 화면이 일반 문구로 떨어진다.
     * @property devTestMsg 개발·점검용 문구. **사용자에게 보여주는 값이 아니다** — 진단으로만 쓴다.
     */
    data class ForceUpdate(
        val minAppVersion: String?,
        val devTestMsg: String?,
    ) : SessionBootstrapState

    /** 판정 완료. [entry] 로 이동하면 된다. */
    data class Resolved(
        val entry: AppEntry,
    ) : SessionBootstrapState
}

/**
 * 앱이 처음 열릴 곳.
 *
 * 계산은 도메인이 하고 실행(백스택 구성)은 호스트가 한다 — 판정에 필요한 건 세션 상태·보류
 * 딥링크·경로 공개 여부뿐이라 화면과 무관하고, 반대로 백스택을 세우는 건 네비게이션 호스트의 일이다.
 */
sealed interface AppEntry {
    /**
     * 딥링크 목적지.
     *
     * 부모 화면까지 함께 깔아야 뒤로가기가 자연스럽다(공지 상세 → 방 홈 → 홈).
     */
    data class DeepLink(
        val route: NavRoute,
    ) : AppEntry

    /** 시작 화면(홈 또는 로그인). 루트라 백스택을 비우고 단독으로 선다. */
    data class Start(
        val route: NavRoute,
    ) : AppEntry
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
        private val pendingDeepLink: PendingDeepLink,
        private val routeAccessPolicy: RouteAccessPolicy,
        private val observability: Observability,
        private val tokenRepository: TokenRepository,
        private val autoLoginUseCase: AutoLoginUseCase,
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

        /**
         * 부팅 시점에 저장된 세션이 있었는지.
         *
         * 로그인 화면에 왔을 때 "첫 설치"와 "세션이 끊겨 돌아옴"을 가르는 근거다. 완주율의 분모가
         * 되는 값이라 둘을 섞으면 지표가 무의미해진다.
         */
        @Volatile
        var hadStoredSession: Boolean = false
            private set

        private val started = AtomicBoolean(false)
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /**
         * 인증 판정과 보류 딥링크를 합쳐 진입 목적지를 정한다.
         *
         * 미인증이면 딥링크를 버리는 게 기본이다 — 세션 없이 목적지를 띄우면 API 가 401 을 받고
         * 사용자는 목적지가 아니라 로그인 화면을 보게 된다. 다만 로그인이 필요 없는 화면이면 그대로 연다.
         *
         * [PendingDeepLink.consume] 은 1회성이라 여기서 한 번만 꺼낸다.
         */
        private fun entry(authenticated: Boolean): AppEntry {
            val pending = pendingDeepLink.consume()
            return when {
                pending == null -> AppEntry.Start(if (authenticated) HomePage.toRoute() else LoginPage.toRoute())

                authenticated || !routeAccessPolicy.requiresLogin(pending.path) -> AppEntry.DeepLink(pending)

                else -> {
                    // 초대 링크로 유입된 신규 사용자가 여기 걸린다. 가입을 마쳐도 목적지로 돌아가지
                    // 않으므로, 잃어버린 진입을 집계해 이어가기 필요성을 판단할 근거를 남긴다.
                    observability.w(TAG) { "인증 전이라 딥링크 유실: path=${pending.path}" }
                    AppEntry.Start(LoginPage.toRoute())
                }
            }
        }

        fun start() {
            if (!started.compareAndSet(false, true)) return
            scope.launch {
                // 버전 게이트 먼저. 페일오픈(null)이면 정상 흐름을 그대로 진행한다.
                val intro = loadIntroUseCase()
                termsVersions = intro?.termsVersions
                val gate = intro?.versionGate
                if (gate?.forceUpdate == true) {
                    _state.value = SessionBootstrapState.ForceUpdate(gate.minAppVersion, gate.devTestMsg)
                    return@launch
                }
                hadStoredSession = tokenRepository.getRefreshToken() != null
                val authenticated = autoLoginUseCase()
                _state.value = SessionBootstrapState.Resolved(entry(authenticated))
            }
        }
    }
