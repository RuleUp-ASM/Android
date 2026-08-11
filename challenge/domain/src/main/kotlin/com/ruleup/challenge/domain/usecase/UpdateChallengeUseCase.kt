package com.ruleup.challenge.domain.usecase

import com.ruleup.challenge.domain.entity.ChallengeUpdate
import com.ruleup.challenge.domain.entity.ChallengeUpdateResult
import com.ruleup.challenge.domain.repository.ChallengeRepository
import javax.inject.Inject

/**
 * 챌린지 수정 (명세 PATCH /challenges/{id}).
 *
 * 잠금 범위는 화면이 `editableFields` 로 먼저 반영하되, **클라이언트 판단을 최종 권위로 보지 않는다** —
 * 409 를 받으면 settings 를 재조회해 다시 그린다.
 */
class UpdateChallengeUseCase
    @Inject
    constructor(
        private val challengeRepository: ChallengeRepository,
    ) {
        suspend operator fun invoke(
            challengeId: String,
            update: ChallengeUpdate,
        ): ChallengeUpdateResult = challengeRepository.update(challengeId, update)
    }
