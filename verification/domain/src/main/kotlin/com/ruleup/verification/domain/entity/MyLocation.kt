package com.ruleup.verification.domain.entity

/** 내 인증 장소(앵커) 조회 결과. */
data class MyLocation(
    val anchors: List<LocationPin>,
    // 앵커 마지막 적용 시각(ISO-8601).
    val appliedFrom: String?,
    // 서버 설정 인증 반경(m)
    val serverRadiusM: Float? = null,
    // 이번 달에 앵커를 바꿀 수 있는지(월 1회).
    val changeAvailable: Boolean = false,
    // 다음 변경 가능 시각(ISO-8601).
    val nextChangeAvailableAt: String? = null,
)
