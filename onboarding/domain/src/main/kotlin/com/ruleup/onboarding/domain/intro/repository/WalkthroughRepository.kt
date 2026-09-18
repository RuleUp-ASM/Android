package com.ruleup.onboarding.domain.intro.repository

/**
 * 워크쓰루를 본 적 있는지 기억한다.
 *
 * **로그아웃해도 지워지지 않는다** — 기기 기준으로 한 번 본 소개를 다시 띄우면, 재로그인할 때마다
 * 3장을 넘겨야 로그인 화면에 닿는다. 그래서 토큰 저장소가 아니라 기기 저장소에 둔다.
 */
interface WalkthroughRepository {
    /** 아직 못 봤으면 false. 읽기에 실패해도 false 로 떨어진다 — 한 번 더 보는 쪽이 못 보는 쪽보다 낫다. */
    suspend fun isSeen(): Boolean

    /** 끝까지 봤거나 건너뛴 시점에 부른다. 여러 번 불려도 안전하다. */
    suspend fun markSeen()
}
