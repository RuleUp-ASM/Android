package com.ruleup.challenge.domain.repository

import com.ruleup.challenge.domain.entity.ChallengeWatchers
import com.ruleup.challenge.domain.entity.WatcherAcceptance
import com.ruleup.challenge.domain.entity.WatcherInvitation
import com.ruleup.challenge.domain.entity.Watching
import com.ruleup.challenge.domain.entity.WatchingUpdate

/**
 * 루틴 실패 패널티 — 감시자 통지.
 *
 * 수락은 **인앱 전용**이다(감시자 테크 스펙 5-2·2026-08-31) — 웹 동의는 폐지됐고 룰업 유저만
 * 감시자가 될 수 있다. 초대 전달은 사용자 본인 채널(카카오톡 공유)로만 하고, 실패 통지 발송은
 * 서버가 담당한다.
 *
 * **감시자 해제는 없다.** 관계는 루틴이 끝나면 배치가 지우고, 받는 쪽은 [updateWatching] 으로
 * 수신을 닫는다. 그래서 지정한 쪽이 관계를 끊는 경로가 계약에 없다.
 *
 * 감시자는 챌린지 × 참여자 단위로 붙는다(발송 대상 = (챌린지, 실패 사용자)의 ACTIVE 감시자).
 */
interface WatcherRepository {
    /**
     * 감시자 초대 생성(명세: POST /challenges/{id}/watchers/invitations). 토큰 7일 만료.
     * 무료 한도(챌린지당 3명) 초과면 [com.ruleup.challenge.domain.entity.WatcherLimitExceededException].
     */
    suspend fun createInvitation(challengeId: String): WatcherInvitation

    /** 내 감시자 목록 조회(명세: GET /challenges/{id}/watchers). 참여자 본인 기준, INVITED 포함 전체(status=ALL). */
    suspend fun getWatchers(challengeId: String): ChallengeWatchers

    /**
     * 내가 감시자로 등록된 관계 목록(명세: GET /users/me/watching). 마이페이지 「내가 받는 알림」.
     *
     * 해제 엔드포인트는 없다 — 관계는 루틴 종료 시 배치가 지우고, 사용자는 수신만 닫는다.
     */
    suspend fun getWatching(): List<Watching>

    /**
     * 내 감시 항목의 수신 설정(명세: PATCH /users/me/watching/{watcherId}).
     *
     * 두 단계가 명확히 다르다 — [pushEnabled] 는 **푸시만** 끄고 알림함 적재는 유지하며,
     * [revoke] 는 완전 수신거부(REVOKED)에 같은 생성자로부터 30일 재초대 차단까지 건다.
     * 서버가 둘의 동시 전송을 400 으로 막으므로 한 번에 하나만 보낸다.
     *
     * 이미 거부한 항목이면 [com.ruleup.challenge.domain.entity.AlreadyRevokedException].
     */
    suspend fun updateWatching(
        watcherId: String,
        pushEnabled: Boolean? = null,
        revoke: Boolean? = null,
    ): WatchingUpdate

    /**
     * 초대 수락(명세: POST /watchers/invitations/{token}/accept). **로그인 필수**이고 수락이 곧
     * 수신 동의다 — 서버가 토큰과 로그인 상태를 함께 확인한 뒤에야 관계가 성립한다.
     *
     * 만료는 [com.ruleup.challenge.domain.entity.InvitationExpiredException],
     * 이미 수락한 초대는 [com.ruleup.challenge.domain.entity.AlreadyConsentedException],
     * 본인 초대는 [com.ruleup.challenge.domain.entity.CannotWatchSelfException] 이 던져진다.
     */
    suspend fun acceptInvitation(token: String): WatcherAcceptance
}
