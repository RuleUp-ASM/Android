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
 * OS 에 사전 등록할 지오펜스 1개 (명세 §2.1). [requestId] = "{userId}#{challengeId}#{anchorIndex}"
 * (멤버 자연키 `uq_member(challenge_id, user_id)` 파생) — GEOFENCE 이벤트의 anchorId 로 서버에 보고된다.
 * 반경은 아래 범위로 클램프된다.
 */
data class GeofenceTarget(
    val requestId: String,
    val lat: Double,
    val lng: Double,
    val radiusM: Float,
    val dwellMinutes: Int,
) {
    companion object {
        // 명세 setup·my-location 앵커 반경 범위(500~5000m)와 동일 — 서버 판정 반경과 OS 트리거 반경을 일치시킨다.
        const val MIN_RADIUS_M: Float = 500f
        const val MAX_RADIUS_M: Float = 5000f
    }
}

/** 지도 핀 결과 페이로드 (명세 §5.3). [label]·[address] 는 표시용("우리 동네 헬스장"). */
data class LocationPin(
    val lat: Double,
    val lng: Double,
    val radiusM: Float,
    val label: String?,
    val address: String? = null,
)
