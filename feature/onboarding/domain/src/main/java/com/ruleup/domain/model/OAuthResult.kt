package com.ruleup.domain.model

/**
 * OAuth 검증 결과. 기존 사용자면 정식 세션을, 신규 사용자면 가입 토큰을 받는다.
 */
sealed interface OAuthResult {
    /** 기존 사용자: 바로 로그인 완료. */
    data class ExistingUser(
        val session: AuthSession,
    ) : OAuthResult

    /** 신규 사용자: 가입 화면으로 진행해야 함. */
    data class NewUser(
        val signupToken: String,
        val signupTokenExpiresInSeconds: Int,
        val oauthProfile: OAuthProfile,
    ) : OAuthResult
}
