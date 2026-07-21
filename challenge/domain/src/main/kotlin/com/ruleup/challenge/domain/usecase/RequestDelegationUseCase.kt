package com.ruleup.challenge.domain.usecase

import com.ruleup.challenge.domain.entity.DelegationTicket
import com.ruleup.challenge.domain.repository.ChallengeRepository
import javax.inject.Inject

/**
 * 방장 위임 요청 생성 (명세: POST /challenges/{id}/delegation, OWNER).
 * 대상은 MANAGER 여야 하며 7일 후 자동 만료된다.
 */
class RequestDelegationUseCase
    @Inject
    constructor(
        private val challengeRepository: ChallengeRepository,
    ) {
        suspend operator fun invoke(
            challengeId: String,
            targetUserId: String,
        ): DelegationTicket = challengeRepository.requestDelegation(challengeId, targetUserId)
    }
