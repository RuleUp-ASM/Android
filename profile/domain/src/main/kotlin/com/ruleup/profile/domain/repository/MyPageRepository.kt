package com.ruleup.profile.domain.repository

import com.ruleup.profile.domain.entity.GroupChallengeSummary
import com.ruleup.profile.domain.entity.MyHome

/**
 * 마이 탭 조회 계층 (마이프로필·캘린더 스펙).
 * 인증 파이프라인이 쌓은 데이터를 읽기 전용으로 조립한 응답을 받는다 — 새 판정 로직 없음.
 */
interface MyPageRepository {
    suspend fun getHome(): MyHome

    /** 참여 중(ACTIVE)인 그룹 챌린지만 — 그룹 랭킹 진입용. */
    suspend fun getMyGroupChallenges(): List<GroupChallengeSummary>
}
