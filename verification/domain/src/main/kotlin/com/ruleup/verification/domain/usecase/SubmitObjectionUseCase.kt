package com.ruleup.verification.domain.usecase

import com.ruleup.verification.domain.entity.ObjectionTicket
import com.ruleup.verification.domain.entity.ObjectionType
import com.ruleup.verification.domain.repository.VerificationRepository
import javax.inject.Inject

/**
 * 이의 제기 제출 (명세: POST /challenges/{id}/objections).
 * 잠정 실패 일자에 대해 본인이 창(3일) 이내 제출한다. 창 경과·비대상 등은 예외로 전파된다.
 */
class SubmitObjectionUseCase
    @Inject
    constructor(
        private val verificationRepository: VerificationRepository,
    ) {
        suspend operator fun invoke(
            challengeId: String,
            targetDate: String,
            content: String,
            type: ObjectionType = ObjectionType.FAILURE,
            imageUrl: String? = null,
        ): ObjectionTicket =
            verificationRepository.submitObjection(
                challengeId = challengeId,
                type = type,
                targetDate = targetDate,
                content = content,
                imageUrl = imageUrl,
            )
    }
