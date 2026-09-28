package com.ruleup.onboarding.domain.auth.usecase

import com.ruleup.domain.helper.LocalUserDataCleaner
import com.ruleup.domain.token.TokenRepository
import com.ruleup.onboarding.domain.auth.entity.Withdrawal
import com.ruleup.onboarding.domain.auth.repository.AuthRepository
import javax.inject.Inject

/** 회원 탈퇴. */
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
            tokenRepository.clear()
            localUserDataCleaner.clear()
            return result
        }
    }
