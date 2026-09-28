package com.ruleup.verification.domain.repository

import com.ruleup.verification.domain.entity.HealthTarget

/** 움직임(HEALTH) 수집 대상 로컬 보관. */
interface HealthTargetStore {
    suspend fun replaceAll(
        targets: Set<HealthTarget>,
        sleepRequested: Boolean,
    )

    suspend fun all(): Set<HealthTarget>

    suspend fun sleepRequested(): Boolean
}
