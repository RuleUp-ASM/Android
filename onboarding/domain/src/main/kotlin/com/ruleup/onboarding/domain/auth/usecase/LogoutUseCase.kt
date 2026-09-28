package com.ruleup.onboarding.domain.auth.usecase

import com.ruleup.domain.helper.LocalUserDataCleaner
import com.ruleup.domain.helper.PushTokenRevoker
import com.ruleup.domain.token.TokenRepository
import com.ruleup.onboarding.domain.auth.repository.AuthRepository
import javax.inject.Inject

/** 로그아웃. */
class LogoutUseCase
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val tokenRepository: TokenRepository,
        private val localUserDataCleaner: LocalUserDataCleaner,
        private val pushTokenRevoker: PushTokenRevoker,
    ) {
        suspend operator fun invoke() {
            // refreshToken revoke 뒤로 미루면 accessToken 만료 시 갱신이 막혀 해제 요청이 401 로 끝난다.
            pushTokenRevoker.revoke()
            tokenRepository.getRefreshToken()?.let { refreshToken ->
                runCatching { authRepository.logout(refreshToken) }
            }
            tokenRepository.clear()
            localUserDataCleaner.clear()
        }
    }
