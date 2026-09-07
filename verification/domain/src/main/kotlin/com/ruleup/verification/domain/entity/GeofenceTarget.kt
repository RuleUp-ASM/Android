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
 *
 * [radiusM] 은 서버가 내려준 인증 반경을 그대로 쓴다 — 서버 판정 반경과 OS 트리거 반경이
 * 어긋나면 안 된다.
 */
data class GeofenceTarget(
    val requestId: String,
    val lat: Double,
    val lng: Double,
    val radiusM: Float,
    val dwellMinutes: Int,
)

/**
 * 지도 핀 결과 페이로드 (명세 §5.3). [label]·[address] 는 표시용("우리 동네 헬스장").
 *
 * **반경은 핀의 속성이 아니다** — 인증 반경은 참여자가 아니라 서버가 정하는 단일값이라
 * 셋업 응답의 `serverRadiusM` 을 따른다(인증 정책 §1.1).
 */
data class LocationPin(
    val lat: Double,
    val lng: Double,
    val label: String?,
    val address: String? = null,
)

/**
 * 제출 단위 앵커 묶음. 개수 제한을 타입이 보장한다 — [of] 를 거치지 않고는 만들 수 없어
 * 어떤 경로로 와도 규칙이 빠지지 않는다.
 *
 * 비어 있어도 된다 — 앱 전용 셋업은 앵커 없이 제출하고 서버가 location 을 생략한다(명세 setup).
 * "1개 이상 추가" 같은 화면별 요구는 그 화면이 판단한다.
 */
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
