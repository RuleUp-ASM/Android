package com.ruleup.profile.domain.entity

/** 피초대자 현황 항목 (명세 invitees[] — status 는 현재 SIGNED_UP 단일 값). */
data class FriendInvitee(
    // visibleNicknameTo 적용된 닉네임 (검수 전 = tempNickname)
    val nickname: String,
    val status: String,
    // 가입 시각 ISO-8601
    val occurredAt: String,
)

/**
 * 친구 초대 정보 (명세: GET /me/invitation).
 * 코드/링크는 유저당 1개 — 없으면 서버가 생성 후 반환(멱등). QR 은 클라가 [inviteUrl] 로 렌더링한다.
 */
data class FriendInvitation(
    // 6자 대문자+숫자
    val inviteCode: String,
    val inviteUrl: String,
    // 보상 안내 문구 (지급 정책 확정 전 — 서버 관리)
    val rewardDescription: String,
    val invitees: List<FriendInvitee>,
)
