package com.ruleup.ui.navigation

import com.ruleup.domain.navigation.NavRoute
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

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
 * [consume] 은 1회성이다. 두 번째 호출은 null 을 돌려준다 — 스플래시가 재구성되어도 같은
 * 목적지로 두 번 이동하지 않는다.
 */
@Singleton
class PendingDeepLink
    @Inject
    constructor() {
        private val pending = AtomicReference<NavRoute?>(null)

        fun set(route: NavRoute?) {
            pending.set(route)
        }

        fun consume(): NavRoute? = pending.getAndSet(null)
    }
