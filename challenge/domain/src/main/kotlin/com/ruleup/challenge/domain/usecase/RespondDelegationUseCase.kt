package com.ruleup.challenge.domain.usecase

import com.ruleup.challenge.domain.entity.DelegationAction
import com.ruleup.challenge.domain.entity.DelegationResolution
import com.ruleup.challenge.domain.repository.ChallengeRepository
import javax.inject.Inject

/**
 * 방장 위임 요청 응답 (명세: PATCH /challenges/{id}/delegation/{delegationId}).
 * ACCEPT/REJECT 는 대상자, CANCEL 은 요청 OWNER.
 *
 * 현재 앱에선 요청자(OWNER)의 CANCEL 에 쓰인다. 대상자의 ACCEPT/REJECT 는 대기 위임 조회 API 부재로
 * 진입점(주로 푸시 딥링크)이 확정되면 연결한다.
 */
class RespondDelegationUseCase
    @Inject
    constructor(
        private val challengeRepository: ChallengeRepository,
    ) {
        suspend operator fun invoke(
            challengeId: String,
            delegationId: String,
            action: DelegationAction,
        ): DelegationResolution = challengeRepository.respondDelegation(challengeId, delegationId, action)
    }
