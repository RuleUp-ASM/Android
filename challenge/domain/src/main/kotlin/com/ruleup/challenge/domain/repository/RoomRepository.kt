package com.ruleup.challenge.domain.repository

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
