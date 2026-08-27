package com.ruleup.onboarding.domain.observability

import javax.inject.Inject
import javax.inject.Singleton

/**
 * `login_attempt` 부터 `signup_complete` 까지의 경과를 잰다 — 가입 소요 시간 지표(중앙값 90초)의 근거다.
 *
 * 로그인 화면과 온보딩 마지막 단계가 서로 다른 ViewModel 이라 값을 넘길 자리가 없다. 가입 흐름은
 * 앱 전체에 하나뿐이므로 싱글턴에 시작 시각만 들고 있는다.
 *
 * 시작 기록이 없으면 [consumeElapsedMillis] 가 null 이다 — 0 을 돌려주면 "0초 만에 가입"이라는 없는 표본이
 * 집계에 섞인다.
 */
@Singleton
class SignupTimer
    @Inject
    constructor() {
        @Volatile
        private var startedAt: Long? = null

        fun start() {
            startedAt = System.currentTimeMillis()
        }

        /** 가입이 끝나면 소비한다. 다음 가입이 앞선 기록을 이어받지 않도록 읽으면서 비운다. */
        fun consumeElapsedMillis(): Long? {
            val started = startedAt ?: return null
            startedAt = null
            return System.currentTimeMillis() - started
        }
    }
