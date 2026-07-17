package com.ruleup.profile.domain.usecase

import com.ruleup.profile.domain.entity.CalendarDayDetail
import com.ruleup.profile.domain.repository.MyPageRepository
import javax.inject.Inject

/**
 * 캘린더 일자 상세 조회 (명세: GET /me/calendar/{date} — VerificationDaily).
 * 일자 탭 시 챌린지별 결과·인증 방식·실패 사유를 보여준다.
 */
class GetCalendarDayDetailUseCase
    @Inject
    constructor(
        private val myPageRepository: MyPageRepository,
    ) {
        suspend operator fun invoke(date: String): CalendarDayDetail = myPageRepository.getCalendarDay(date)
    }
