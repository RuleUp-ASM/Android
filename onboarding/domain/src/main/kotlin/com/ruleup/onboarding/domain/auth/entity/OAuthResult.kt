package com.ruleup.onboarding.domain.auth.entity

/** `POST /auth/oauth/{provider}` 의 두 갈래. */
sealed interface OAuthResult {
    /** 기존 회원. */
    data class ExistingUser(
        val session: AuthSession,
        val restored: Boolean,
    ) : OAuthResult

    /** 신규 회원. */
    data class NewUser(
        val signupToken: String,
        val expiresInSeconds: Int,
        val profile: OAuthProfile,
    ) : OAuthResult
}

/** IdP 가 준 프로필 힌트. */
data class OAuthProfile(
    val email: String?,
    val nicknameHint: String?,
    val profileImageUrlHint: String?,
)
