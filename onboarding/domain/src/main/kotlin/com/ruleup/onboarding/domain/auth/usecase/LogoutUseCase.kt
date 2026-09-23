package com.ruleup.onboarding.domain.auth.usecase

import com.ruleup.domain.helper.LocalUserDataCleaner
import com.ruleup.domain.token.TokenRepository
import com.ruleup.onboarding.domain.auth.repository.AuthRepository
import javax.inject.Inject

/**
 * 로그아웃. 서버에 현재 기기 refreshToken revoke 를 요청(명세 4.5)한 뒤 로컬 토큰과 **단말에 남은
 * 사용자 데이터를** 지운다 — revoke 가 실패해도 로컬 로그아웃은 진행한다.
 *
 * 토큰만 지우면 자동인증 수집 버퍼와 OS 지오펜스가 그대로 남아, 다음 사용자가 앞 사용자의 신호를
 * 자기 것으로 올린다(AUTH-10).
 */
class LogoutUseCase
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val tokenRepository: TokenRepository,
        private val localUserDataCleaner: LocalUserDataCleaner,
    ) {
        suspend operator fun invoke() {
            tokenRepository.getRefreshToken()?.let { refreshToken ->
                runCatching { authRepository.logout(refreshToken) }
            }
            localUserDataCleaner.clear()
            tokenRepository.clear()
        }
    }
