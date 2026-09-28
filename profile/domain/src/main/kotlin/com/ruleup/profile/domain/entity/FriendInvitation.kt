package com.ruleup.profile.domain.entity

/** 피초대자 현황 항목. */
data class FriendInvitee(
    // visibleNicknameTo 적용된 닉네임 (검수 전 = tempNickname)
    val nickname: String,
    val status: String,
    // 가입 시각 ISO-8601
    val occurredAt: String,
)

/** 친구 초대 정보. */
data class FriendInvitation(
    // 6자 대문자+숫자
    val inviteCode: String,
    val inviteUrl: String,
    // 보상 안내 문구 (지급 정책 확정 전 — 서버 관리)
    val rewardDescription: String,
    val invitees: List<FriendInvitee>,
)
