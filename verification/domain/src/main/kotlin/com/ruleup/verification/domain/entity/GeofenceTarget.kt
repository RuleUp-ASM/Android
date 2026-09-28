package com.ruleup.verification.domain.entity

/** 좌표 바인딩 방식. */
enum class LocationBinding {
    SHARED,
    PER_MEMBER,
    NONE,
    ;

    companion object {
        fun fromValue(value: String?): LocationBinding = entries.find { it.name == value } ?: NONE
    }
}

/** OS 에 사전 등록할 지오펜스 1개. */
data class GeofenceTarget(
    val requestId: String,
    val lat: Double,
    val lng: Double,
    val radiusM: Float,
    val dwellMinutes: Int,
)

/** 지도 핀 결과 페이로드. */
data class LocationPin(
    val lat: Double,
    val lng: Double,
    val label: String?,
    val address: String? = null,
)

/** 제출 단위 앵커 묶음. */
class AnchorSet private constructor(
    val pins: List<LocationPin>,
) {
    val isEmpty: Boolean get() = pins.isEmpty()

    companion object {
        val EMPTY = AnchorSet(emptyList())

        fun of(pins: List<LocationPin>): AnchorSet {
            require(pins.size <= SetupAnchors.MAX_COUNT) {
                "인증 장소는 최대 ${SetupAnchors.MAX_COUNT}개까지 추가할 수 있어요"
            }
            return AnchorSet(pins)
        }
    }
}
