package com.ruleup.profile.domain.repository

import com.ruleup.profile.domain.entity.ActivityCalendar
import com.ruleup.profile.domain.entity.CalendarDayDetail
import com.ruleup.profile.domain.entity.GroupChallengeSummary
import com.ruleup.profile.domain.entity.MyHome
import com.ruleup.profile.domain.entity.ReputationDetail
import com.ruleup.profile.domain.entity.ReputationHistory

/**
 * 마이 탭 조회 계층 (마이프로필·캘린더 스펙).
 * 인증 파이프라인이 쌓은 데이터를 읽기 전용으로 조립한 응답을 받는다 — 새 판정 로직 없음.
 */
interface MyPageRepository {
    suspend fun getHome(): MyHome

    /** 참여 중(ACTIVE)인 그룹 챌린지만 — 그룹 랭킹 진입용. */
    suspend fun getMyGroupChallenges(): List<GroupChallengeSummary>

    suspend fun getReputation(): ReputationDetail

    suspend fun getReputationHistory(): ReputationHistory

    /** [month] = YYYY-MM. */
    suspend fun getCalendar(month: String): ActivityCalendar

    /** [date] = YYYY-MM-DD. */
    suspend fun getCalendarDay(date: String): CalendarDayDetail
}
