package com.ruleup.onboarding.domain.entity

/** `POST /auth/oauth/{provider}` 의 두 갈래. `isNewUser` 로 나뉜다. */
sealed interface OAuthResult {
    /**
     * 기존 회원. 앱 토큰이 바로 나온다.
     *
     * @property restored 탈퇴 1년 내 재가입으로 계정이 복원된 경우 true. 제재 이력도 함께 복원되며,
     *   닉네임을 남이 선점했으면 `user.nicknameStatus` 가 `CONFLICT` 로 온다.
     */
    data class ExistingUser(
        val session: AuthSession,
        val restored: Boolean,
    ) : OAuthResult

    /**
     * 신규 회원. 계정은 아직 만들어지지 않았고 [signupToken] 으로 가입을 마쳐야 한다.
     *
     * @property expiresInSeconds 5분. 만료되면 로그인부터 다시 시작한다.
     */
    data class NewUser(
        val signupToken: String,
        val expiresInSeconds: Int,
        val profile: OAuthProfile,
    ) : OAuthResult
}

/**
 * IdP 가 준 프로필 힌트. **프리필 용도이며 자동 제출하지 않는다** — 닉네임은 check API 를 통과해야
 * 한다.
 *
 * 생일·성별 힌트는 두지 않는다. 카카오는 비즈 앱 미전환, 구글은 기본 scope 밖이라 서버가 항상
 * null 로 고정했고(2026-08-03), 온보딩에서 직접 입력받는다.
 */
data class OAuthProfile(
    val email: String?,
    val nicknameHint: String?,
    val profileImageUrlHint: String?,
)
