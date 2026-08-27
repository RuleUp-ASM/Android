package com.ruleup.challenge.domain.repository

import com.ruleup.challenge.domain.entity.ChallengeWaitlist

/**
 * 정원이 찬 방의 대기열 (명세 미확정 — `/challenges/{id}/waitlist` 로 가정).
 *
 * 자리가 나면 서버가 선착순으로 자동 참여시킨다 — 앱은 폴링하거나 가입을 재시도하지 않는다.
 */
interface WaitlistRepository {
    suspend fun getWaitlist(challengeId: String): ChallengeWaitlist

    /**
     * 대기열에 줄을 선다. 상한을 넘겼으면
     * [com.ruleup.challenge.domain.entity.WaitlistFullException], 그 사이 방이 시작됐으면
     * [com.ruleup.challenge.domain.entity.WaitlistClosedException] 가 올라온다.
     */
    suspend fun enterWaitlist(challengeId: String): ChallengeWaitlist

    /** 대기 취소. 참여료가 있었으면 환불도 서버가 같이 처리한다. */
    suspend fun leaveWaitlist(challengeId: String)
}
