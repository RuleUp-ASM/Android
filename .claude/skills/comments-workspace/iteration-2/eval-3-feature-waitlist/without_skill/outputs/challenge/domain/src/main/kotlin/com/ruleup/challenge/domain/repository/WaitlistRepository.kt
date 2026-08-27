package com.ruleup.challenge.domain.repository

import com.ruleup.challenge.domain.entity.WaitlistEntry
import com.ruleup.challenge.domain.entity.WaitlistExit
import com.ruleup.challenge.domain.entity.WaitlistStatus

/**
 * 정원이 찬 방의 대기열.
 *
 * 자리가 나면 **서버가 선착순으로 자동 참여**시킨다 — 앱이 승격을 요청하는 경로는 없고, 상태를 다시
 * 받아 결과를 확인할 뿐이다. 그래서 이 포트에는 "승격" 메서드가 없다.
 *
 * 참여([ChallengeRepository.join])와 나눠 둔 이유는 생애주기가 다르기 때문이다 — 참여는 한 번의
 * 요청으로 끝나지만 대기는 끝날 때까지 방 밖에서 상태를 들고 있어야 한다.
 */
interface WaitlistRepository {
    /**
     * 방의 대기열 현황 조회. 정원이 찬 방의 상세에서만 부른다.
     *
     * 참여 버튼을 "대기하기 / 대기 중(순번) / 막힘" 중 무엇으로 그릴지는 전부
     * [WaitlistStatus] 가 결정한다 — 화면이 50% 한도를 다시 계산하지 않는다.
     */
    suspend fun getWaitlist(challengeId: String): WaitlistStatus

    /**
     * 대기열에 들어간다.
     *
     * 응답의 순번은 서버 계산이 끝나기 전이면 null 로 오고
     * [com.ruleup.challenge.domain.entity.WaitlistPosition.Calculating] 으로 옮겨진다 —
     * 이 경우 화면은 "확인 중"이고 **1번으로 그리지 않는다**.
     *
     * 대기 한도(정원의 50%)를 넘으면
     * [com.ruleup.challenge.domain.entity.WaitlistFullException],
     * 그 사이 방이 시작됐으면 [com.ruleup.challenge.domain.entity.WaitlistClosedException] 이 던져진다.
     */
    suspend fun enqueue(challengeId: String): WaitlistEntry

    /**
     * 대기를 취소한다. 참여료를 냈다면 환불 결과가 [WaitlistExit.refund] 로 함께 온다.
     *
     * 취소가 도착하기 전에 이미 승격됐다면 서버는 취소가 아니라 그 사실을 알려준다 —
     * [WaitlistExit.reason] 이 [com.ruleup.challenge.domain.entity.WaitlistExitReason.PROMOTED] 이므로
     * 화면은 "취소됐어요" 대신 방으로 안내한다.
     */
    suspend fun cancel(challengeId: String): WaitlistExit
}
