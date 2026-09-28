package com.ruleup.verification.domain.repository

import com.ruleup.verification.domain.entity.GeofenceTarget

/** 지오펜스 등록 포트. */
interface GeofenceRegister {
    /** 현재 OS 등록 펜스와 활성 [targets] 를 비교해 차집합만 add/remove 한다. */
    suspend fun reconcile(targets: List<GeofenceTarget>)

    /** 로컬에 보존된 목표 전체를 [reconcile] 로 재등록한다. */
    suspend fun reconcilePersisted()

    /** 한 멤버([requestIdPrefix] 로 시작하는 requestId 묶음)의 목표 전체를 등록/갱신한다. */
    suspend fun bind(
        requestIdPrefix: String,
        targets: List<GeofenceTarget>,
    )

    /** [requestIdPrefix] 로 시작하는 목표 묶음을 해제한다(챌린지 탈퇴 등). */
    suspend fun unbind(requestIdPrefix: String)

    /** 등록된 모든 펜스를 해제한다(로그아웃 등). */
    suspend fun clear()
}
