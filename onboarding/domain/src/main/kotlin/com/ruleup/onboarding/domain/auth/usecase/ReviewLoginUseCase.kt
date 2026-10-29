package com.ruleup.onboarding.domain.auth.usecase

import com.ruleup.domain.token.TokenRepository
import com.ruleup.onboarding.domain.auth.entity.AuthSession
import com.ruleup.onboarding.domain.auth.repository.AuthRepository
import javax.inject.Inject

class ReviewLoginUseCase
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val tokens: TokenRepository,
    ) {
        suspend operator fun invoke(code: String): AuthSession {
            val session = authRepository.reviewLogin(code)
            tokens.saveReviewSession(session.token, session.user.id)
            return session
        }
    }
