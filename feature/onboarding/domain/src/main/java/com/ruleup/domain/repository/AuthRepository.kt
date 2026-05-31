package com.ruleup.domain.repository

import com.ruleup.domain.model.Agreements
import com.ruleup.domain.model.AuthSession
import com.ruleup.domain.model.InterestCategory
import com.ruleup.domain.model.NicknameAvailability
import com.ruleup.domain.model.OAuthProvider
import com.ruleup.domain.model.OAuthResult
import com.ruleup.domain.model.Tokens

interface AuthRepository {

    /**
     * OAuth authorization_code 를 검증한다.
     * 기존 사용자면 [OAuthResult.ExistingUser], 신규면 [OAuthResult.NewUser] 를 반환한다.
     */
    suspend fun oauthLogin(
        provider: OAuthProvider,
        code: String,
        codeVerifier: String,
        redirectUri: String,
    ): OAuthResult

    /** signup_token 으로 신규 가입을 완료하고 정식 세션을 받는다. */
    suspend fun signup(
        signupToken: String,
        nickname: String,
        interestCategories: List<InterestCategory>,
        profileImageUrl: String?,
        agreements: Agreements,
    ): AuthSession

    /** Refresh Token 으로 새 토큰 페어를 발급받는다. */
    suspend fun refresh(refreshToken: String): Tokens

    /** 현재 디바이스의 Refresh Token 을 revoke 한다. */
    suspend fun logout(refreshToken: String)

    /** 닉네임 사용 가능 여부를 사전 확인한다. */
    suspend fun checkNicknameAvailability(nickname: String): NicknameAvailability
}
