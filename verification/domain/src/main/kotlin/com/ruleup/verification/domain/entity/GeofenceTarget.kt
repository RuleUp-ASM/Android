package com.ruleup.verification.domain.entity

/**
 * 좌표 바인딩 방식 (명세 §5.1). 루틴 템플릿 플래그로 분기한다 — 카테고리로 추측 금지.
 * - [SHARED]: 생성자(또는 방장)가 핀 → verification_config.GPS
 * - [PER_MEMBER]: 멤버가 참여 시 각자 핀 → challenge_members.geofence_*
 * - [NONE]: 지도 생략(앱사용·화면·WAKE 루틴)
 */
enum class LocationBinding {
    SHARED,
    PER_MEMBER,
    NONE,
    ;

    companion object {
        fun fromValue(value: String?): LocationBinding = entries.find { it.name == value } ?: NONE
    }
}

/**
 * OS 에 사전 등록할 지오펜스 1개 (명세 §2.1). [requestId] = "{challengeMemberId}#{anchorIndex}" —
 * GEOFENCE 이벤트의 anchorId 로 서버에 보고된다. 반경은 아래 범위로 클램프된다.
 */
data class GeofenceTarget(
    val requestId: String,
    val lat: Double,
    val lng: Double,
    val radiusM: Float,
    val dwellMinutes: Int,
) {
    companion object {
        const val MIN_RADIUS_M: Float = 50f
        const val MAX_RADIUS_M: Float = 1000f
    }
}

/** 지도 핀 결과 페이로드 (명세 §5.3). [label] 은 표시용("우리 동네 헬스장"). */
data class LocationPin(
    val lat: Double,
    val lng: Double,
    val radiusM: Float,
    val label: String?,
)
