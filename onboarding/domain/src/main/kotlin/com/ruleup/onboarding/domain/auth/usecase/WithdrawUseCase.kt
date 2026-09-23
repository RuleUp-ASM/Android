package com.ruleup.onboarding.domain.auth.usecase

import com.ruleup.domain.helper.LocalUserDataCleaner
import com.ruleup.domain.token.TokenRepository
import com.ruleup.onboarding.domain.auth.entity.Withdrawal
import com.ruleup.onboarding.domain.auth.repository.AuthRepository
import javax.inject.Inject

/**
 * 회원 탈퇴. 서버 처리가 끝난 **뒤에만** 로컬 토큰을 지운다.
 *
 * 순서가 규칙이다 — 먼저 지우면 요청에 실을 accessToken 이 사라져 탈퇴가 안 되고, 사용자는
 * 로그아웃만 된 채 계정이 남는다. 로그아웃([LogoutUseCase])이 실패해도 토큰을 지우는 것과 반대다.
 */
class WithdrawUseCase
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val tokenRepository: TokenRepository,
        private val localUserDataCleaner: LocalUserDataCleaner,
    ) {
        suspend operator fun invoke(): Withdrawal {
            val result = authRepository.withdraw(Withdrawal.CONFIRM_PHRASE)
            // 탈퇴는 계정이 사라지는 것이라 수집 버퍼가 남을 이유가 더더욱 없다.
            localUserDataCleaner.clear()
            tokenRepository.clear()
            return result
        }
    }
