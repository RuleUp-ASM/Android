package com.ruleup.verification.domain.usecase

import com.ruleup.verification.domain.entity.ManualMethod
import com.ruleup.verification.domain.entity.ManualSubmitResult
import com.ruleup.verification.domain.port.VerificationRepository
import dev.zacsweers.metro.Inject

/**
 * 수동 인증 당일 제출(명세 3.4·§9, VF-04). SELF_CHECK 우선, PHOTO 는 [imageUrl] 필수.
 * [asFallback]=true 면 자동인데 오늘 자동이 안 될 때의 예비 폴백(주1회·잠정성공·이의윈도우, §9.2).
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
            asFallback: Boolean = false,
        ): ManualSubmitResult =
            verificationRepository.submitManual(challengeId, method, targetDate, imageUrl, asFallback)
    }
