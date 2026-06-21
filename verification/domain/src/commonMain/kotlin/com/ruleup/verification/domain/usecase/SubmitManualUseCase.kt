package com.ruleup.verification.domain.usecase

import com.ruleup.verification.domain.entity.ManualMethod
import com.ruleup.verification.domain.entity.ManualSubmitResult
import com.ruleup.verification.domain.port.VerificationRepository
import dev.zacsweers.metro.Inject

/**
 * 수동 인증 당일 제출(명세 3.4, VF-04 후순위). SELF_CHECK 우선, PHOTO 는 [imageUrl] 필수.
 */
class SubmitManualUseCase
    @Inject
    constructor(
        private val verificationRepository: VerificationRepository,
    ) {
        suspend operator fun invoke(
            challengeId: String,
            method: ManualMethod,
            targetDate: String? = null,
            imageUrl: String? = null,
        ): ManualSubmitResult = verificationRepository.submitManual(challengeId, method, targetDate, imageUrl)
    }
