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

    /**
     * 한 멤버([requestIdPrefix] 로 시작하는 requestId 묶음)의 목표 전체를 등록/갱신한다.
     * 새 [targets] 에 없는 기존 목표는 해제하고, 다른 멤버의 목표는 유지한다. 지도 핀 확정 시 호출(명세 §5).
     */
    suspend fun bind(
        requestIdPrefix: String,
        targets: List<GeofenceTarget>,
    )

    /** [requestIdPrefix] 로 시작하는 목표 묶음을 해제한다(챌린지 탈퇴 등). */
    suspend fun unbind(requestIdPrefix: String)

    /** 등록된 모든 펜스를 해제한다(로그아웃 등). */
    suspend fun clear()
}
