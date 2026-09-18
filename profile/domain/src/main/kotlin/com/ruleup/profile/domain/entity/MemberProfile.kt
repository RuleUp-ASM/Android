package com.ruleup.profile.domain.entity

import com.ruleup.domain.entity.user.Tier

/**
 * 타인 프로필 (명세: GET /users/{userId}/profile).
 *
 * **공개 범위가 좁다** — 닉네임·사진·표시 티어·완주 개수뿐이다. 진행 중 목록·통계·티어 점수·
 * 캘린더는 응답에 아예 없다. 화면이 "비공개"라고 말할 수 있는 근거가 이 타입의 좁음이다.
 *
 * [tier] 는 표시 티어이고 **점수는 오지 않는다.** 내 티어 화면처럼 점수를 곁들이면 안 된다.
 *
 * @param withdrawn 탈퇴한 사용자. 닉네임·사진이 의미를 잃으므로 화면이 "탈퇴한 사용자"로 덮는다.
 * @param blocked 내가 차단한 사용자. 서버가 이미 임시 닉네임·기본 이미지로 마스킹해 내려주고,
 *   화면은 거기에 해제 경로를 더한다 — 차단해 둔 사실을 모르면 왜 이름이 이상한지 알 수 없다.
 */
data class MemberProfile(
    val userId: String,
    val nickname: String,
    val profileImageUrl: String?,
    val tier: Tier,
    val completedChallengeCount: Int,
    val withdrawn: Boolean,
    val blocked: Boolean,
)
