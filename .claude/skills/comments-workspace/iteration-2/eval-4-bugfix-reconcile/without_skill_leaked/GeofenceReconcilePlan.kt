package com.ruleup.verification.data.signal.geofence

/**
 * 로컬 보존 목표와 OS 등록분을 맞춰 재등록 계획을 만든다(명세 §2.3).
 * OS 등록은 부팅·Play Services 갱신으로 휘발하므로 콜드스타트마다 부른다.
 */
fun reconcilePlan(
    desired: List<GeofenceTarget>,
    registered: List<GeofenceTarget>,
): ReconcilePlan {
    val registeredById = registered.associateBy { it.requestId }
    // requestId 는 멤버·앵커 순번에서 파생돼 좌표를 옮겨도 그대로다 — 등록 여부만 보면 옛 자리 펜스가 남는다.
    // 앵커까지 비교해 바뀐 것도 다시 넣는다: addGeofences 가 같은 requestId 를 교체하므로 제거 없이 갱신된다.
    val toAdd = desired.filterNot { it == registeredById[it.requestId] }
    val desiredIds = desired.mapTo(HashSet()) { it.requestId }
    val toRemoveIds = registered.map { it.requestId }.filterNot { it in desiredIds }
    return ReconcilePlan(toAdd = toAdd, toRemoveIds = toRemoveIds)
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
