package com.ruleup.challenge.domain.usecase

import com.ruleup.challenge.domain.entity.ChallengeRanking
import com.ruleup.challenge.domain.repository.RoomRepository
import javax.inject.Inject

/**
 * 그룹 랭킹 조회 (명세: GET /challenges/{id}/ranking).
 * 서버가 확정일 기준 비정규화 진행률을 정렬만 해서 내려준다 — 상위 3 포디움 + 전체 + 내 순위.
 */
class GetChallengeRankingUseCase
    @Inject
    constructor(
        private val roomRepository: RoomRepository,
    ) {
        suspend operator fun invoke(challengeId: String): ChallengeRanking = roomRepository.getRanking(challengeId)
    }
