package com.ruleup.domain.navigation

import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/** [PendingDeepLink.consumeFor] 의 결과. */
sealed interface PendingDeepLinkEntry {
    /** 보류된 목적지가 없다. 평소 진입이다. */
    data object None : PendingDeepLinkEntry

    /** 지금 열어도 되는 목적지. */
    data class Open(
        val route: NavRoute,
    ) : PendingDeepLinkEntry

    /** 로그인이 필요한 화면인데 아직 미인증이다. 목적지는 보관한 채로 두고 [PendingDeepLink.consumeAfterLogin] 이 연다. */
    data class Deferred(
        val route: NavRoute,
    ) : PendingDeepLinkEntry
}

/**
 * 인증이 끝나면 이동할 딥링크 목적지를 잠깐 보관한다.
 *
 * 딥링크는 인증보다 먼저 도착한다. 그렇다고 목적지를 백스택에 먼저 깔면 **세션 없이 화면이 떠서**
 * API 가 401 을 받고, 토큰 정리가 일어나면 사용자는 목적지가 아니라 로그인 화면으로 튕긴다.
 * 그래서 시작 스택은 항상 스플래시로 두고, 목적지는 여기 보관했다가 자동 로그인 성공 후 꺼낸다.
 *
 * 백스택에 실어 나르지 않는 이유는 둘이다 — 목적지 인자와 스플래시 인자가 섞이고, 직렬화되어
 * 프로세스 사망 뒤에도 남는다. 프로세스가 죽으면 백스택 자체가 복원되므로 보류분은 오히려
 * 사라지는 게 맞다.
 *
 * [consumeFor] 는 1회성이다. 두 번째 호출은 [PendingDeepLinkEntry.None] 이다 — 스플래시가
 * 재구성되어도 같은 목적지로 두 번 이동하지 않는다.
 */
@Singleton
class PendingDeepLink
    @Inject
    constructor() {
        private val pending = AtomicReference<NavRoute?>(null)

        fun set(route: NavRoute?) {
            pending.set(route)
        }

        /**
         * 인증 상태에 비추어 지금 열어도 되는 목적지인지까지 판정해 꺼낸다.
         *
         * 미인증이면 목적지를 열지 않고 보관한다 — 세션 없이 띄우면 API 가 401 을 받는다. 초대 링크로 온
         * 사용자는 로그인·가입을 마친 뒤 [consumeAfterLogin] 으로 그 목적지에 도착한다.
         */
        fun consumeFor(
            authenticated: Boolean,
            policy: RouteAccessPolicy,
        ): PendingDeepLinkEntry {
            val route = pending.get() ?: return PendingDeepLinkEntry.None
            if (!authenticated && policy.requiresLogin(route.path)) return PendingDeepLinkEntry.Deferred(route)
            pending.compareAndSet(route, null)
            return PendingDeepLinkEntry.Open(route)
        }

        /** 로그인·가입 완료 직후 보관해 둔 목적지를 한 번 꺼낸다. 없으면 null — 평소처럼 홈으로 간다. */
        fun consumeAfterLogin(): NavRoute? = pending.getAndSet(null)
    }
