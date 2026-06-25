package com.ruleup.verification.domain.port

/**
 * SCREEN_TIME 대상 패키지 로컬 보관(명세 §3.2 스코핑). 챌린지 참여/설정 시 채우고
 * sync 스코프([SyncScopeProvider])가 읽는다. WAKE(잠금해제)는 패키지와 무관하게 수집된다.
 */
interface UsageTargetStore {
    suspend fun replaceAll(packages: Set<String>)

    suspend fun all(): Set<String>
}
