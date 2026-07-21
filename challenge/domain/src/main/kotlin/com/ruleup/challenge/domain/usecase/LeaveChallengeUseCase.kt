package com.ruleup.challenge.domain.usecase

import com.ruleup.challenge.domain.entity.LeaveResult
import com.ruleup.challenge.domain.repository.ChallengeRepository
import javax.inject.Inject

/**
 * 챌린지 탈퇴 (명세: DELETE /challenges/{id}/members/me).
 * 본인 success 이력이 있으면 탈퇴 패널티가 트리거되고([LeaveResult.penaltyApplied]), 재참여는 영구 불가.
 * OWNER 는 탈퇴 불가 — 서버가 403 OWNER_CANNOT_LEAVE 로 분기 사유를 준다.
 */
class LeaveChallengeUseCase
    @Inject
    constructor(
        private val challengeRepository: ChallengeRepository,
    ) {
        suspend operator fun invoke(challengeId: String): LeaveResult = challengeRepository.leaveChallenge(challengeId)
    }
