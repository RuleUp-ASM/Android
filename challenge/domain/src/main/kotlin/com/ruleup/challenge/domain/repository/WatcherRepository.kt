package com.ruleup.challenge.domain.repository

import com.ruleup.challenge.domain.entity.ChallengeWatchers
import com.ruleup.challenge.domain.entity.WatcherInvitation
import com.ruleup.challenge.domain.entity.WatcherInvitationInfo

/**
 * 루틴 실패 패널티 — 감시자 통지(감시자 초대·수락·관리).
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

    /** 초대 링크 진입 — 토큰 검증 + 초대 정보 조회(명세: GET /watchers/invitations/{token}). */
    suspend fun getInvitation(token: String): WatcherInvitationInfo

    /**
     * 감시자 수락(명세: POST /watchers/invitations/{token}/accept).
     * 룰업 유저의 인앱 수락 = 수신동의. 채널은 인앱 푸시로 등록된다.
     */
    suspend fun acceptInvitation(token: String)
}
