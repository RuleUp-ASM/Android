package com.ruleup.onboarding.domain.logging

import javax.inject.Inject
import javax.inject.Singleton

/** `login_attempt` 부터 `signup_complete` 까지의 경과를 잰다 */
@Singleton
class SignupTimer
    @Inject
    constructor() {
        @Volatile
        private var startedAt: Long? = null

        fun start() {
            startedAt = System.currentTimeMillis()
        }

        /** 가입이 끝나면 소비한다. */
        fun consumeElapsedMillis(): Long? {
            val started = startedAt ?: return null
            startedAt = null
            return System.currentTimeMillis() - started
        }
    }
