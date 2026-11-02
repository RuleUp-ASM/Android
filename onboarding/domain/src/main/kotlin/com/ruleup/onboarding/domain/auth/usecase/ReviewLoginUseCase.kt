package com.ruleup.onboarding.domain.auth.usecase

import com.ruleup.domain.token.TokenRepository
import com.ruleup.onboarding.domain.auth.entity.LoginOutcome
import com.ruleup.onboarding.domain.auth.repository.AuthRepository
import javax.inject.Inject

class ReviewLoginUseCase
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val tokens: TokenRepository,
    ) {
        suspend operator fun invoke(code: String): LoginOutcome {
            val result = authRepository.reviewLogin(code)
            val user = result.session.user
            tokens.saveReviewSession(result.session.token, user.id)
            return LoginOutcome.forExistingUser(user.nickname, requireNotNull(user.account), result.restored)
        }
    }
