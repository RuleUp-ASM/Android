package com.ruleup.verification.domain.repository

import com.ruleup.verification.domain.entity.SignalScope

/**
 * OS 신호 수집 포트 (driven adapter, 명세 §1·§2). 도메인은 본 포트만 알고 Android 구현이 채운다
 * (테스트·iOS 대비). Geofence 전이는 리시버가 비동기로 버퍼에 적재하고, 본 포트는 sync 시점에
 * UsageStats 델타·보조 측위 1발을 [scope] 범위로 수집해 로컬 버퍼에 적재한다.
 */
interface SignalCollector {
    /** 활성 챌린지 스코프로 최신 OS 신호를 수집해 로컬 버퍼([SignalRepository])에 적재한다. */
    suspend fun capture(scope: SignalScope)
}
