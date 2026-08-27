package com.ruleup.verification.data.signal.geofence

/**
 * 로컬 보존 목표와 직전 등록분을 맞춰 재등록 계획을 만든다(명세 §2.3).
 * OS 등록은 부팅·Play Services 갱신으로 휘발하므로 콜드스타트마다 부른다.
 *
 * [registeredIds] 는 "지금 OS 에 살아 있는 펜스"가 아니라 **직전에 등록을 시도해 로컬에 보존한
 * requestId 집합**이다. 지오펜싱 API 에는 등록분 조회가 없어서 OS 의 실제 상태는 알 수 없다.
 * 그래서 이 집합은 **제거 대상을 구하는 용도로만** 쓴다.
 *
 * 추가는 차집합을 쓰지 않고 [desired] 전체를 그대로 등록한다. 이유가 둘이다:
 *  1. requestId 는 앵커가 아니라 멤버(userId#challengeId#index)에 붙는 키다. 인증 장소를 옮겨
 *     좌표·반경·체류시간이 바뀌어도 requestId 는 그대로라, 차집합으로 거르면 바뀐 목표가
 *     "이미 등록됨"으로 취급돼 OS 에는 옛 좌표가 그대로 남는다.
 *  2. 재부팅·Play Services 갱신으로 OS 등록분이 통째로 비어도 로컬 집합은 남아 있어서,
 *     차집합을 쓰면 아무것도 재등록하지 않고 펜스가 전부 죽는다.
 *
 * addGeofences 는 동일 requestId 를 멱등 교체하므로 전체 재등록이 안전하다. 바뀐 목표는
 * 교체로 갱신되고, 사라진 목표만 [toRemoveIds] 로 해제한다. 교체될 id 를 제거 목록에 넣지
 * 않는 것도 의도된 것이다 — remove 와 add 는 별개 비동기 호출이라 순서가 뒤집히면 방금 등록한
 * 펜스를 제거가 지워버린다.
 */
fun reconcilePlan(
    desired: List<GeofenceTarget>,
    registeredIds: Set<String>,
): ReconcilePlan {
    val desiredIds = desired.mapTo(HashSet()) { it.requestId }
    val toRemove = registeredIds.filter { it !in desiredIds }
    return ReconcilePlan(toAdd = desired, toRemoveIds = toRemove)
}

data class ReconcilePlan(
    val toAdd: List<GeofenceTarget>,
    val toRemoveIds: List<String>,
)

data class GeofenceTarget(
    val requestId: String,
    val lat: Double,
    val lng: Double,
    val radiusM: Float,
    val dwellMinutes: Int,
)
