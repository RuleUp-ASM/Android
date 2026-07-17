package com.ruleup.profile.domain.usecase

import com.ruleup.profile.domain.entity.StatsPeriod
import com.ruleup.profile.domain.entity.StatsReport
import com.ruleup.profile.domain.repository.MyPageRepository
import javax.inject.Inject

/**
 * 통계 리포트 조회 (명세: GET /me/stats?period). anchor 는 생략 = 오늘(KST) 기준.
 * 총 완주·평균 완주율·매너 상승분·평균 연속일·완주율 시리즈·인사이트를 받는다.
 */
class GetStatsReportUseCase
    @Inject
    constructor(
        private val myPageRepository: MyPageRepository,
    ) {
        suspend operator fun invoke(period: StatsPeriod): StatsReport = myPageRepository.getStats(period)
    }
