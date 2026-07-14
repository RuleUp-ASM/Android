package com.ruleup.verification.domain.repository

import com.ruleup.verification.domain.entity.GeofenceTarget

/**
 * 지오펜스 등록 포트 (명세 §2.1). OS 에 좌표를 사전 등록해 zero-touch presence 를 만든다.
 * 지오펜스는 재부팅·위치설정 토글·앱 데이터 삭제 시 휘발되므로(명세 §2.3) [reconcile] 을
 * BOOT_COMPLETED·콜드스타트·참여/탈퇴 트리거에서 호출한다.
 */
interface GeofenceRegistrar {
    /**
     * 현재 OS 등록 펜스와 활성 [targets] 를 비교해 차집합만 add/remove 한다.
     * 최대 100개 제한(TOO_MANY_GEOFENCES) 초과 시 마감 임박 우선으로 등록한다(명세 §2.1).
     */
    suspend fun reconcile(targets: List<GeofenceTarget>)

    /** 로컬에 보존된 목표 전체를 [reconcile] 로 재등록한다. BOOT_COMPLETED·콜드스타트에서 호출한다. */
    suspend fun reconcilePersisted()

    /** 단일 목표를 등록/갱신한다(다른 목표는 유지). 지도 핀 확정 시 호출(명세 §5). */
    suspend fun bind(target: GeofenceTarget)

    /** 단일 목표를 해제한다(챌린지 탈퇴 등). */
    suspend fun unbind(requestId: String)

    /** 등록된 모든 펜스를 해제한다(로그아웃 등). */
    suspend fun clear()
}
