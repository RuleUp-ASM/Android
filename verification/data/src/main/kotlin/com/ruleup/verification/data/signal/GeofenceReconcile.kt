package com.ruleup.verification.data.signal

import com.ruleup.verification.domain.entity.GeofenceTarget

/**
 * 지오펜스 재조정의 순수 로직(테스트 가능). OS 는 등록된 펜스 목록 조회 API 가 없으므로
 * 로컬에 보존한 직전 목표(desired)와 새 목표의 차집합으로 제거 대상을 구한다(명세 §2.1).
 * 추가는 addGeofences 가 동일 requestId 를 멱등 교체하므로 새 목표 전체를 그대로 등록한다
 * (재부팅 후 OS 가 비어도 전부 재등록됨).
 */
internal object GeofenceReconcile {
    fun toRemove(
        previous: Set<String>,
        targets: List<GeofenceTarget>,
    ): List<String> {
        val desired = targets.mapTo(HashSet()) { it.requestId }
        return previous.filter { it !in desired }
    }
}
