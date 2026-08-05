package com.ruleup.onboarding.domain.auth.usecase

import com.ruleup.domain.token.TokenRepository
import javax.inject.Inject

/**
 * 로그인 화면에 온 사용자가 첫 설치인지 재로그인인지.
 *
 * 완주율의 분모라 둘을 섞으면 지표가 무의미해진다. 판단 근거는 로그아웃해도 남는 로그인 이력이다
 * ([TokenRepository.hasEverLoggedIn]).
 */
class HasEverLoggedInUseCase
    @Inject
    constructor(
        private val tokenRepository: TokenRepository,
    ) {
        suspend operator fun invoke(): Boolean = tokenRepository.hasEverLoggedIn()
    }
