package com.ruleup.verification.data.signal.geofence

/**
 * 로컬 보존 목표와 OS 등록분을 맞춰 재등록 계획을 만든다(명세 §2.3).
 * OS 등록은 부팅·Play Services 갱신으로 휘발하므로 콜드스타트마다 부른다.
 *
 * [registered] 에는 직전에 등록한 목표를 좌표까지 그대로 넘긴다 — OS 는 등록분을 되돌려주지
 * 않으므로 requestId 만 들고 있으면 앵커가 옮겨진 걸 알 방법이 없다.
 */
fun reconcilePlan(
    desired: List<GeofenceTarget>,
    registered: List<GeofenceTarget>,
): ReconcilePlan {
    val registeredById = registered.associateBy { it.requestId }
    // 같은 requestId 여도 좌표·반경·체류가 다르면 다시 넣는다. addGeofences 가 같은 id 를
    // 덮어쓰는 게 앵커를 옮기는 유일한 방법이고, 빼먹으면 옛 자리에서 계속 터진다.
    val toAdd = desired.filter { registeredById[it.requestId] != it }
    val toRemoveIds = registeredById.keys.filter { id -> desired.none { it.requestId == id } }
    return ReconcilePlan(toAdd = toAdd, toRemoveIds = toRemoveIds)
}

data class ReconcilePlan(
    val toAdd: List<GeofenceTarget>,
    val toRemoveIds: List<String>,
)

// requestId 에 좌표를 섞지 않는다 — 이 값은 GEOFENCE 이벤트의 anchorId 로 서버에 보고된다.
data class GeofenceTarget(
    val requestId: String,
    val lat: Double,
    val lng: Double,
    val radiusM: Float,
    val dwellMinutes: Int,
)
