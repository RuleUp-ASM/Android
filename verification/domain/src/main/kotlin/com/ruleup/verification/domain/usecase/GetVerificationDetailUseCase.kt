package com.ruleup.verification.domain.usecase

import com.ruleup.verification.domain.entity.VerificationDetail
import com.ruleup.verification.domain.repository.VerificationRepository
import javax.inject.Inject

/**
 * 상세/검증 결과 진입 시 챌린지 인증 여부 판단 조회(명세 3.3, VF-03).
 * 성공·실패 화면을 한 응답으로 렌더한다.
 */
class GetVerificationDetailUseCase
    @Inject
    constructor(
        private val verificationRepository: VerificationRepository,
    ) {
        suspend operator fun invoke(
            challengeId: String,
            logDays: Int = 7,
        ): VerificationDetail = verificationRepository.getVerificationDetail(challengeId, logDays)
    }
