package com.ruleup.verification.data.signal.geofence

import com.ruleup.verification.domain.entity.GeofenceTarget

/** OS 는 등록된 펜스 목록 조회 API 가 없어, 로컬에 보존한 직전 목표와의 차집합으로 제거 대상을 구한다. */
internal object GeofenceReconcile {
    fun toRemove(
        previous: Set<String>,
        targets: List<GeofenceTarget>,
    ): List<String> {
        val desired = targets.mapTo(HashSet()) { it.requestId }
        return previous.filter { it !in desired }
    }
}
