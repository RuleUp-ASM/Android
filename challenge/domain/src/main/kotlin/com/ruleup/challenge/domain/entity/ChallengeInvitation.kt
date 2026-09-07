package com.ruleup.challenge.domain.entity

import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.entity.user.Tier

/**
 * 멤버 초대 링크 (명세: POST /challenges/{id}/invitations).
 *
 * 비공개 방의 **유일한 입장 수단**이다. 서버는 토큰·링크만 만들고 전달은 발급자가 본인 채널로
 * 직접 한다 — 감시자 초대와 같은 패턴이다.
 *
 * [inviteUrl] 은 앱링크 도메인(`https://android.ruleup.co.kr/c/{token}`)이라 설치자는 바로 앱으로
 * 들어온다. 클라가 조립하지 않고 서버가 준 값을 그대로 공유한다 — 경로 규약이 바뀌어도 앱을
 * 고칠 필요가 없다.
 */
data class ChallengeInvitation(
    val invitationId: String,
    val token: String,
    val inviteUrl: String,
    // 발급 +7일
    val expiresAt: String?,
)

/** 초대 링크가 가리키는 방 요약 (명세 `challenge`). 가입 전에 보여 줄 만큼만 온다. */
data class InvitedChallenge(
    val challengeId: String,
    val title: String,
    val imageUrl: String?,
    val category: Category?,
    val participantCount: Int,
    val capacity: Int,
    val minTier: Tier?,
    val startDate: String?,
    val endDate: String?,
)

/**
 * 초대 링크 미리보기 (명세: GET /challenges/invitations/{token}).
 *
 * [joinable] 이 false 면 [blockReason] 이 이유를 말한다 — 수락 버튼을 누르기 **전에** 막힌 이유를
 * 보여 주려고 서버가 미리 판정해 준다. 토큰은 이 조회로 소모되지 않는다.
 */
data class ChallengeInvitationPreview(
    val invitationId: String,
    val challenge: InvitedChallenge,
    val inviterNickname: String?,
    val joinable: Boolean,
    val blockReason: JoinBlockReason?,
    val expiresAt: String?,
)
