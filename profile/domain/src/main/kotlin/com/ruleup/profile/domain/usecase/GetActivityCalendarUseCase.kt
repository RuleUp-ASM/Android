package com.ruleup.profile.domain.usecase

import com.ruleup.profile.domain.entity.ActivityCalendar
import com.ruleup.profile.domain.repository.MyPageRepository
import javax.inject.Inject

/**
 * 활동 캘린더 월 조회 (명세: GET /me/calendar?month=YYYY-MM).
 * 판정 대상일만 내려오며(day status 는 서버 계산), 응답에 없는 날짜는 비대상일로 렌더링한다.
 */
class GetActivityCalendarUseCase
    @Inject
    constructor(
        private val myPageRepository: MyPageRepository,
    ) {
        suspend operator fun invoke(month: String): ActivityCalendar = myPageRepository.getCalendar(month)
    }
