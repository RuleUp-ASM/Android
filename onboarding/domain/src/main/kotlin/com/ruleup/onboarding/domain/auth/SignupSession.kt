package com.ruleup.onboarding.domain.auth

import com.ruleup.onboarding.domain.auth.entity.OAuthProfile
import javax.inject.Inject
import javax.inject.Singleton

/** 가입이 끝날 때까지만 사는 signup_token 보관소. */
@Singleton
class SignupSession
    @Inject
    constructor() {
        @Volatile
        private var token: String? = null

        @Volatile
        private var profile: OAuthProfile? = null

        fun start(
            signupToken: String,
            oauthProfile: OAuthProfile,
        ) {
            token = signupToken
            profile = oauthProfile
        }

        /** 진행 중인 가입의 토큰. */
        fun token(): String? = token

        /** IdP 가 준 프로필 힌트. */
        fun oauthProfile(): OAuthProfile? = profile

        /** 가입 완료·이탈 시 비운다. */
        fun clear() {
            token = null
            profile = null
        }
    }
