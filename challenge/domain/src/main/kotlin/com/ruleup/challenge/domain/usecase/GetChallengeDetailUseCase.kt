package com.ruleup.challenge.domain.usecase

import com.ruleup.challenge.domain.entity.ChallengeDetail
import com.ruleup.challenge.domain.repository.ChallengeRepository
import javax.inject.Inject

/**
 * 챌린지 상세 + 참여 자격 조회(명세 3.3). 홈 카드 탭 → 상세 화면 진입 시 호출한다.
 */
class GetChallengeDetailUseCase
    @Inject
    constructor(
        private val challengeRepository: ChallengeRepository,
    ) {
        suspend operator fun invoke(challengeId: String): ChallengeDetail = challengeRepository.getChallenge(challengeId)
    }
