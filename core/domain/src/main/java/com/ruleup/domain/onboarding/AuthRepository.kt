package com.ruleup.domain.onboarding

import com.ruleup.entity.onboarding.OAuthAuthorization
import com.ruleup.entity.onboarding.OAuthResult
import com.ruleup.entity.user.Agreement
import com.ruleup.entity.user.AuthSession
import com.ruleup.entity.user.InterestCategory
import com.ruleup.entity.user.Token

interface AuthRepository {
    /** 소셜 로그인. 기존/신규 분기를 반환한다(명세 4.1/4.2). */
    suspend fun exchangeToken(authorization: OAuthAuthorization): OAuthResult

    /**
     * 신규 가입 완료. 약관 동의([agreements])를 clientProperties 로 함께 전달한다(명세 4.3).
     */
    suspend fun signup(
        signupToken: String,
        nickname: String,
        interestCategories: List<InterestCategory>,
        profileImageUrl: String?,
        agreements: Agreement,
    ): AuthSession

    /** 앱 토큰 재발급(명세 4.4). */
    suspend fun refreshToken(refreshToken: String): Token

    /** 현재 기기 refreshToken revoke(명세 4.5). */
    suspend fun logout(refreshToken: String)
}
