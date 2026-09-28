package com.ruleup.domain.navigation

import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/** [PendingDeepLink.consumeFor] 의 결과. */
sealed interface PendingDeepLinkEntry {
    /** 보류된 목적지가 없다. */
    data object None : PendingDeepLinkEntry

    /** 지금 열어도 되는 목적지. */
    data class Open(
        val route: NavRoute,
    ) : PendingDeepLinkEntry

    /** 로그인이 필요한 화면인데 아직 미인증이다. */
    data class Deferred(
        val route: NavRoute,
    ) : PendingDeepLinkEntry
}

/** 인증이 끝나면 이동할 딥링크 목적지를 잠깐 보관한다. */
@Singleton
class PendingDeepLink
    @Inject
    constructor() {
        private val pending = AtomicReference<NavRoute?>(null)

        fun set(route: NavRoute?) {
            pending.set(route)
        }

        /** 인증 상태에 비추어 지금 열어도 되는 목적지인지까지 판정해 꺼낸다. */
        fun consumeFor(
            authenticated: Boolean,
            policy: RouteAccessPolicy,
        ): PendingDeepLinkEntry {
            val route = pending.get() ?: return PendingDeepLinkEntry.None
            if (!authenticated && policy.requiresLogin(route.path)) return PendingDeepLinkEntry.Deferred(route)
            pending.compareAndSet(route, null)
            return PendingDeepLinkEntry.Open(route)
        }

        /** 로그인·가입 완료 직후 보관해 둔 목적지를 한 번 꺼낸다. */
        fun consumeAfterLogin(): NavRoute? = pending.getAndSet(null)
    }
