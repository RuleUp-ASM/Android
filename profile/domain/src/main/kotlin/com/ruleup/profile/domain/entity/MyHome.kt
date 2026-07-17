package com.ruleup.profile.domain.entity

/**
 * 닉네임 검수 상태 (명세 nicknameStatus — LLM 비동기 검수).
 * 본인 화면에는 검수 상태와 무관하게 본인이 정한 닉네임을 그대로 보여주고, 뱃지로만 상태를 알린다.
 */
enum class NicknameStatus(
    val value: String,
) {
    PENDING("PENDING"),
    APPROVED("APPROVED"),
    REJECTED("REJECTED"),
    ;

    companion object {
        // 미지 값은 뱃지 없는 APPROVED 취급 (서버 enum 확장 대비)
        fun fromValue(value: String?): NicknameStatus = entries.find { it.value == value } ?: APPROVED
    }
}

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
