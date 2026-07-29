package com.ruleup.challenge.domain.repository

import com.ruleup.challenge.domain.entity.ChallengeWatchers
import com.ruleup.challenge.domain.entity.WatcherInvitation

/**
 * 루틴 실패 패널티 — 감시자 통지(감시자 초대 생성·관리).
 *
 * 초대 **수락**은 앱이 하지 않는다 — 비유저 감시자를 포함해 웹 동의 페이지가 담당한다.
 * 앱은 초대를 만들어 사용자 본인 채널로 공유하는 데까지만 관여한다.
 * 감시자는 챌린지 × 참여자 단위로 붙는다(발송 대상 = (챌린지, 실패 사용자)의 ACTIVE 감시자).
 * 초대 전달은 사용자 본인 채널(카카오톡 공유)로만 하고, 실패 통지 발송은 서버가 담당한다.
 */
interface WatcherRepository {
    /**
     * 감시자 초대 생성(명세: POST /challenges/{id}/watchers/invitations). 토큰 7일 만료.
     * 무료 한도(챌린지당 3명) 초과면 [com.ruleup.challenge.domain.entity.WatcherLimitExceededException].
     */
    suspend fun createInvitation(challengeId: String): WatcherInvitation

    /** 내 감시자 목록 조회(명세: GET /challenges/{id}/watchers). 참여자 본인 기준, INVITED 포함 전체(status=ALL). */
    suspend fun getWatchers(challengeId: String): ChallengeWatchers

    /** 감시자 해제(명세: DELETE /challenges/{id}/watchers/{watcherId}). REVOKED + 연락처 파기. */
    suspend fun removeWatcher(
        challengeId: String,
        watcherId: String,
    )
}
