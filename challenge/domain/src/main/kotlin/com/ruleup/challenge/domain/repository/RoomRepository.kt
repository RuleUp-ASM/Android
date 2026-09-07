package com.ruleup.challenge.domain.repository

import com.ruleup.challenge.domain.entity.ChallengeCalendar
import com.ruleup.challenge.domain.entity.ChallengeRanking
import com.ruleup.challenge.domain.entity.ChallengeRoom
import com.ruleup.challenge.domain.entity.ChallengeThreads
import com.ruleup.challenge.domain.entity.CrossChallengeRanking
import com.ruleup.challenge.domain.entity.RankingMode
import com.ruleup.challenge.domain.entity.ThreadPolicy

/**
 * 챌린지 방 내부 (방 홈·피드·랭킹) 조회. 방 안 API는 ACTIVE 멤버 전용(403 NOT_A_MEMBER)이다.
 */
interface RoomRepository {
    suspend fun getRoom(challengeId: String): ChallengeRoom

    /** 방 스레드 피드. [cursor] 가 null 이면 첫 페이지다. */
    suspend fun getThreads(
        challengeId: String,
        cursor: String? = null,
        size: Int = ThreadPolicy.PAGE_SIZE,
    ): ChallengeThreads

    suspend fun getRanking(challengeId: String): ChallengeRanking

    /**
     * 챌린지 월 캘린더 (명세: GET /challenges/{id}/calendar — 2026-09-07 신규).
     * [month] 는 `YYYY-MM`.
     *
     * 계정 단위 캘린더(`/me/calendar`)와 **상태 enum 이 다르다** — 한 방으로 좁히면 하루 판정
     * 대상이 1건이라 부분 성공이 없다. 그래서 별도 엔드포인트다.
     *
     * 참여한 적 없는 방이면 403 `NOT_CHALLENGE_MEMBER` 다. 완료·이탈한 방은 조회된다.
     */
    suspend fun getCalendar(
        challengeId: String,
        month: String,
    ): ChallengeCalendar

    /**
     * 방 밖 랭킹 — 같은 모드의 방끼리 비교한다. 멤버 전용이 아니며 하루 1회 배치 스냅샷이다.
     * [challengeId] 를 주면 응답의 myChallenge 로 내 방 하이라이트가 채워진다.
     */
    suspend fun getCrossRanking(
        mode: RankingMode,
        challengeId: String? = null,
        cursor: String? = null,
        size: Int? = null,
    ): CrossChallengeRanking
}
