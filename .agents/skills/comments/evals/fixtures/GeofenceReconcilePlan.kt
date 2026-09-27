package com.ruleup.verification.data.signal.geofence

/**
 * 로컬 보존 목표와 OS 등록분을 맞춰 재등록 계획을 만든다(명세 §2.3).
 * OS 등록은 부팅·Play Services 갱신으로 휘발하므로 콜드스타트마다 부른다.
 */
fun reconcilePlan(
    desired: List<GeofenceTarget>,
    registeredIds: Set<String>,
): ReconcilePlan {
    val toAdd = desired.filter { it.requestId !in registeredIds }
    val toRemove = registeredIds.filter { id -> desired.none { it.requestId == id } }
    return ReconcilePlan(toAdd = toAdd, toRemoveIds = toRemove)
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
