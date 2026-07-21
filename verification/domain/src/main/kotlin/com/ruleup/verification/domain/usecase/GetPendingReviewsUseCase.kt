package com.ruleup.verification.domain.usecase

import com.ruleup.verification.domain.entity.PendingReviews
import com.ruleup.verification.domain.repository.VerificationRepository
import javax.inject.Inject

/**
 * 방장/공동 관리자 확인 대기함 조회 (명세: GET /challenges/{id}/pending-reviews).
 * 폴백 수동 인증·이의 제기를 통합한 목록을 반환한다.
 */
class GetPendingReviewsUseCase
    @Inject
    constructor(
        private val verificationRepository: VerificationRepository,
    ) {
        suspend operator fun invoke(challengeId: String): PendingReviews = verificationRepository.getPendingReviews(challengeId)
    }
