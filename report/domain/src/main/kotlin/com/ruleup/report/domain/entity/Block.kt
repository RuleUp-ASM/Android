package com.ruleup.report.domain.entity

/**
 * 내가 차단한 목록(명세 GET /users/me/blocks).
 *
 * 차단은 **내 화면에만** 적용되는 개인 설정이라 이 목록도 나만 볼 수 있다. 신고하면 자동으로
 * 쌓이므로 "차단하기" API 는 따로 없다 — 차단을 거는 유일한 경로가 신고다.
 */
data class BlockList(
    val users: List<BlockedUser>,
    val challenges: List<BlockedChallenge>,
) {
    /** 양쪽이 모두 비었는지 — 빈 상태 문구를 가르는 기준. */
    val isEmpty: Boolean
        get() = users.isEmpty() && challenges.isEmpty()
}

/**
 * 차단한 사용자.
 *
 * 실제 닉네임 대신 임시 닉네임이 온다 — 차단해 놓고 목록에서 원본을 다시 보여주면 가리는 의미가 없다.
 */
data class BlockedUser(
    val userId: String,
    val maskedNickname: String,
    // ISO datetime. 서버가 비워 보내면 "언제 차단했는지"만 빠지고 해제는 그대로 된다.
    val blockedAt: String?,
)

/** 차단한 챌린지. */
data class BlockedChallenge(
    val challengeId: String,
    val maskedTitle: String,
    // 참여 중이면 탐색에서 지우는 대신 제목·이미지만 가린다(HiddenEffect.CHALLENGE_MASKED).
    val participating: Boolean,
    val blockedAt: String?,
)
