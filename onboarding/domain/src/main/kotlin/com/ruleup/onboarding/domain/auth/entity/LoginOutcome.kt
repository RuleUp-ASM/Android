package com.ruleup.onboarding.domain.auth.entity

import com.ruleup.domain.entity.user.LockInfo

/** 소셜 로그인 후 화면이 가야 할 곳. */
sealed interface LoginOutcome {
    /** 홈으로. */
    data class GoHome(
        val restored: Boolean,
    ) : LoginOutcome

    /** 제한이 걸린 계정. */
    data class Restricted(
        val lockInfo: LockInfo?,
    ) : LoginOutcome

    /** 닉네임 재설정 강제. */
    data class ResetNickname(
        val currentNickname: String,
    ) : LoginOutcome

    /** 신규 가입. */
    data class GoSignup(
        val signupToken: String,
        val expiresInSeconds: Int,
        val profile: OAuthProfile,
    ) : LoginOutcome
}
