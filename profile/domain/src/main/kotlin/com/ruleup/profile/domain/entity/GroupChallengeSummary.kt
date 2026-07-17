package com.ruleup.profile.domain.entity

/**
 * 그룹 랭킹 진입용 내 그룹 챌린지 요약 (명세: GET /challenges — 마이 스펙 "그룹 랭킹은 방 내부 스펙 재사용").
 * 마이 홈의 그룹 랭킹 메뉴가 챌린지를 골라 랭킹 화면(challengeId 필요)으로 넘어가기 위한 최소 정보다.
 */
data class GroupChallengeSummary(
    val challengeId: String,
    val title: String,
)
