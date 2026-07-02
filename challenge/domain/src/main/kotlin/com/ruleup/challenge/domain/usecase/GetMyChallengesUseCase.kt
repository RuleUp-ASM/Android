package com.ruleup.challenge.domain.usecase

import com.ruleup.challenge.domain.ChallengeRepository
import com.ruleup.challenge.domain.entity.MyChallenge
import com.ruleup.challenge.domain.entity.MyChallengeScope
import javax.inject.Inject

/**
 * 내가 참여 중인 챌린지 목록 조회(명세: GET /challenges). 홈 진입 시 호출해 카드로 노출한다.
 * 기본 [MyChallengeScope.ACTIVE]. 진행률(verification/progress)과 병합해 진행바를 채운다.
 */
class GetMyChallengesUseCase
    @Inject
    constructor(
        private val challengeRepository: ChallengeRepository,
    ) {
        suspend operator fun invoke(scope: MyChallengeScope = MyChallengeScope.ACTIVE): List<MyChallenge> =
            challengeRepository.getMyChallenges(scope)
    }
