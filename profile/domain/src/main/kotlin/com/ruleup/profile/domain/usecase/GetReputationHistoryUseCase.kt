package com.ruleup.profile.domain.usecase

import com.ruleup.profile.domain.entity.ReputationHistory
import com.ruleup.profile.domain.repository.MyPageRepository
import javax.inject.Inject

/**
 * 평판 히스토리 조회 (명세: GET /me/reputation/history).
 * 역대 최고 온도와 마일스톤 피드(append-only, 시간 역순, 상한 50건)를 받는다 — 페이지네이션 없음.
 */
class GetReputationHistoryUseCase
    @Inject
    constructor(
        private val myPageRepository: MyPageRepository,
    ) {
        suspend operator fun invoke(): ReputationHistory = myPageRepository.getReputationHistory()
    }
