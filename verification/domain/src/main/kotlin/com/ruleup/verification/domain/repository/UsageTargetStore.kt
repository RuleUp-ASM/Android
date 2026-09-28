package com.ruleup.verification.domain.repository

/** SCREEN_TIME 대상 패키지 로컬 보관. */
interface UsageTargetStore {
    suspend fun replaceAll(targets: Map<String, Set<String>>)

    /** [challengeId] 의 대상 앱을 통째로 바꾼다. */
    suspend fun replaceFor(
        challengeId: String,
        packages: Set<String>,
    )

    /** 참여 중인 모든 방의 대상을 합친 수집 스코프. */
    suspend fun all(): Set<String>
}
