package com.ruleup.profile.domain.entity

import com.ruleup.domain.entity.user.NicknameStatus

/** 마이 홈 카운트 (명세 counts — 완주·진행 중·그룹). */
data class MyHomeCounts(
    val completed: Int,
    val inProgress: Int,
    val groups: Int,
)

/** 마이 홈 일괄 조회 (명세: GET /me/home). 마이 탭 메인 렌더링용. */
data class MyHome(
    val nickname: String,
    val nicknameStatus: NicknameStatus,
    val profileImageUrl: String?,
    // 현재 매너 온도 (DECIMAL(5,2))
    val mannerTemperature: Double,
    val counts: MyHomeCounts,
)
