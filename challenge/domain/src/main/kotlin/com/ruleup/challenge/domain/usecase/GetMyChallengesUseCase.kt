package com.ruleup.challenge.domain.usecase

import com.ruleup.challenge.domain.entity.MyChallenge
import com.ruleup.challenge.domain.repository.ChallengeRepository
import javax.inject.Inject

/**
 * 내가 참여 중인 챌린지 목록 조회(명세: GET /challenges). 홈 진입 시 호출해 카드로 노출한다.
 * 승인제 폐기로 전량 반환. 진행률(verification/progress)과 병합해 진행바를 채운다.
 */
class GetMyChallengesUseCase
    @Inject
    constructor(
        private val challengeRepository: ChallengeRepository,
    ) {
        suspend operator fun invoke(): List<MyChallenge> = challengeRepository.getMyChallenges()
    }
