package com.ruleup.profile.domain.usecase

import com.ruleup.profile.domain.entity.ReputationDetail
import com.ruleup.profile.domain.repository.MyPageRepository
import javax.inject.Inject

/**
 * 매너 온도 상세 조회 (명세: GET /me/reputation).
 * 현재 온도·다음 목표 진행률·최근 변동(일별 스냅샷 diff 10건)을 받는다 — 온도 계산은 서버 배치 소관.
 */
class GetReputationDetailUseCase
    @Inject
    constructor(
        private val myPageRepository: MyPageRepository,
    ) {
        suspend operator fun invoke(): ReputationDetail = myPageRepository.getReputation()
    }
