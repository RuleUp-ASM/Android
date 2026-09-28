package com.ruleup.onboarding.domain.auth.repository

import com.ruleup.domain.device.DeviceIdentity
import com.ruleup.domain.token.RefreshedSession
import com.ruleup.onboarding.domain.auth.entity.AuthSession
import com.ruleup.onboarding.domain.auth.entity.OAuthAuthorization
import com.ruleup.onboarding.domain.auth.entity.OAuthResult
import com.ruleup.onboarding.domain.auth.entity.PermissionSnapshot
import com.ruleup.onboarding.domain.auth.entity.SignupForm
import com.ruleup.onboarding.domain.auth.entity.Withdrawal

interface AuthRepository {
    /** 소셜 로그인. */
    suspend fun exchangeToken(
        authorization: OAuthAuthorization,
        device: DeviceIdentity,
        permissions: PermissionSnapshot? = null,
    ): OAuthResult

    /** 신규 가입 완료. */
    suspend fun signup(
        form: SignupForm,
        device: DeviceIdentity,
    ): AuthSession

    /** 앱 토큰 재발급(회전). */
    suspend fun refreshToken(refreshToken: String): RefreshedSession

    /** 현재 기기 refreshToken revoke. */
    suspend fun logout(refreshToken: String)

    /** 회원 탈퇴. */
    suspend fun withdraw(confirmPhrase: String): Withdrawal
}
