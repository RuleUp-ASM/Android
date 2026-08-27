package com.ruleup.onboarding.domain.auth.repository

import com.ruleup.domain.token.RefreshedSession
import com.ruleup.onboarding.domain.auth.entity.AuthSession
import com.ruleup.onboarding.domain.auth.entity.DeviceIdentity
import com.ruleup.onboarding.domain.auth.entity.OAuthAuthorization
import com.ruleup.onboarding.domain.auth.entity.OAuthResult
import com.ruleup.onboarding.domain.auth.entity.PermissionSnapshot
import com.ruleup.onboarding.domain.auth.entity.SignupForm

interface AuthRepository {
    /**
     * 소셜 로그인. [device] 는 단일 활성 기기 판정과 동일 설치 다계정 차단에 쓰여 필수고,
     * [permissions] 는 참고용 스냅샷이라 없어도 된다.
     */
    suspend fun exchangeToken(
        authorization: OAuthAuthorization,
        device: DeviceIdentity,
        permissions: PermissionSnapshot? = null,
    ): OAuthResult

    /** 신규 가입 완료. 프로필 사진은 여기서 받지 않는다 — 가입 후 발급받은 토큰으로 별도 API 에 올린다. */
    suspend fun signup(
        form: SignupForm,
        device: DeviceIdentity,
    ): AuthSession

    /** 앱 토큰 재발급(회전). 응답에 userId 가 함께 와서 갱신만으로 세션이 완성된다. */
    suspend fun refreshToken(refreshToken: String): RefreshedSession

    /** 현재 기기 refreshToken revoke. */
    suspend fun logout(refreshToken: String)
}
