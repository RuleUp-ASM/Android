package com.ruleup.verification.domain.port

import com.ruleup.verification.domain.entity.HealthTarget

/**
 * 움직임(HEALTH) 수집 대상 로컬 보관(명세 §3.2 스코핑·§8). 챌린지 셋업 시 채우고
 * sync 스코프([SyncScopeProvider])가 읽어 Health Connect 읽기 범위를 좁힌다.
 * [sleepRequested] 는 수면 인증 챌린지 유무(SLEEP 수집 on/off, 명세 §6.2).
 */
interface HealthTargetStore {
    suspend fun replaceAll(
        targets: Set<HealthTarget>,
        sleepRequested: Boolean,
    )

    suspend fun all(): Set<HealthTarget>

    suspend fun sleepRequested(): Boolean
}
