package com.ruleup.verification.domain.port

import com.ruleup.verification.domain.entity.DeviceIntro

/**
 * Phase 0 인트로 페이로드 채집 포트(전송 스펙 §0.3). 정적 디바이스 프로필 + 최초 권한 스냅샷.
 * Android Build/권한 API 의존이라 data 가 구현한다.
 */
fun interface DeviceIntroProvider {
    suspend fun capture(): DeviceIntro
}
