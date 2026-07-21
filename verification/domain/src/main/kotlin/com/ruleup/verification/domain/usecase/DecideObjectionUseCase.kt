package com.ruleup.verification.domain.usecase

import com.ruleup.verification.domain.entity.ObjectionDecision
import com.ruleup.verification.domain.entity.ObjectionDecisionResult
import com.ruleup.verification.domain.repository.VerificationRepository
import javax.inject.Inject

/**
 * 이의 제기 승인/기각 (명세: POST /challenges/{id}/objections/{objectionId}/decision).
 * 방장·공동 관리자만. 승인 시 SUCCESS·OBJECTION 으로 확정된다.
 */
class DecideObjectionUseCase
    @Inject
    constructor(
        private val verificationRepository: VerificationRepository,
    ) {
        suspend operator fun invoke(
            challengeId: String,
            objectionId: String,
            decision: ObjectionDecision,
            reason: String? = null,
        ): ObjectionDecisionResult = verificationRepository.decideObjection(challengeId, objectionId, decision, reason)
    }
