package com.ruleup.onboarding.domain.intro.repository

/** 워크쓰루를 본 적 있는지 기억한다. */
interface WalkthroughRepository {
    /** 아직 못 봤으면 false. */
    suspend fun isSeen(): Boolean

    /** 끝까지 봤거나 건너뛴 시점에 부른다. */
    suspend fun markSeen()
}
