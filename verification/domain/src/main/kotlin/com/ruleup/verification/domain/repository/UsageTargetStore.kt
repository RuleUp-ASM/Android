package com.ruleup.verification.domain.repository

/**
 * SCREEN_TIME 대상 패키지 로컬 보관(명세 §3.2 스코핑). 챌린지 참여/설정 시 채우고
 * sync 스코프([SyncScopeProvider])가 읽는다. WAKE(잠금해제)는 패키지와 무관하게 수집된다.
 *
 * **수집기가 읽는 곳이 여기 하나다.** 화면이 고른 앱을 여기 넣지 않으면 서버 저장은 성공하는데
 * 단말은 아무것도 모아 보내지 않아, 사용자에게는 "등록했는데 신호가 없는" 상태로 보인다(SETUP-07 · SIG-07).
 */
interface UsageTargetStore {
    /**
     * [challengeId] 의 대상 앱을 통째로 바꾼다. 다른 방의 대상은 건드리지 않는다.
     *
     * 빈 집합이면 그 방의 대상만 지운다 — 대상이 아닌 앱의 사용 기록을 계속 모으지 않기 위해서다.
     */
    suspend fun replaceFor(
        challengeId: String,
        packages: Set<String>,
    )

    /** 참여 중인 모든 방의 대상을 합친 수집 스코프. */
    suspend fun all(): Set<String>
}
