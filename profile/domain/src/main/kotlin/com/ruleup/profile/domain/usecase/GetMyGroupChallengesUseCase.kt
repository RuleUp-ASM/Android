package com.ruleup.profile.domain.usecase

import com.ruleup.profile.domain.entity.GroupChallengeSummary
import com.ruleup.profile.domain.repository.MyPageRepository
import javax.inject.Inject

/**
 * 참여 중(ACTIVE)인 그룹 챌린지 목록 조회 — 마이 홈 "그룹 랭킹" 메뉴의 챌린지 선택용.
 * 랭킹 화면은 challengeId 단위(방 내부 스펙)라 마이에서 진입하려면 선택 단계가 필요하다.
 */
class GetMyGroupChallengesUseCase
    @Inject
    constructor(
        private val myPageRepository: MyPageRepository,
    ) {
        suspend operator fun invoke(): List<GroupChallengeSummary> = myPageRepository.getMyGroupChallenges()
    }
