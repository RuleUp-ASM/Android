package com.ruleup.challenge.domain.usecase

import com.ruleup.challenge.domain.entity.DeleteResult
import com.ruleup.challenge.domain.repository.ChallengeRepository
import javax.inject.Inject

/**
 * 챌린지 삭제 (명세: DELETE /challenges/{id}, 생성자만).
 * 참여자(방장 제외) 0명일 때만 가능하며, 진행 중 + success 이력이 있으면 탈퇴 패널티가 트리거된다
 * ([DeleteResult.penaltyApplied]).
 */
class DeleteChallengeUseCase
    @Inject
    constructor(
        private val challengeRepository: ChallengeRepository,
    ) {
        suspend operator fun invoke(challengeId: String): DeleteResult = challengeRepository.delete(challengeId)
    }
