package com.ruleup.challenge.domain.usecase

import com.ruleup.challenge.domain.entity.ChallengeMembers
import com.ruleup.challenge.domain.repository.ChallengeRepository
import javax.inject.Inject

/**
 * 챌린지 멤버 목록 조회 (명세: GET /challenges/{id}/members).
 * 승인제 폐기로 확정 멤버만 반환한다. 방 홈에서 멤버 섹션 렌더링에 사용한다.
 */
class GetChallengeMembersUseCase
    @Inject
    constructor(
        private val challengeRepository: ChallengeRepository,
    ) {
        suspend operator fun invoke(challengeId: String): ChallengeMembers = challengeRepository.getMembers(challengeId)
    }
