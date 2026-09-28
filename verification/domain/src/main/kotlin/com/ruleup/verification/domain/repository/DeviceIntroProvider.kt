package com.ruleup.verification.domain.repository

import com.ruleup.verification.domain.entity.DeviceIntro

/** Phase 0 인트로 페이로드 채집 포트. */
fun interface DeviceIntroProvider {
    suspend fun capture(): DeviceIntro
}
