package com.ruleup.challenge.domain.repository

import com.ruleup.challenge.domain.entity.ChallengeCalendar
import com.ruleup.challenge.domain.entity.ChallengeRanking
import com.ruleup.challenge.domain.entity.ChallengeRoom
import com.ruleup.challenge.domain.entity.ChallengeThreads
import com.ruleup.challenge.domain.entity.CrossChallengeRanking
import com.ruleup.challenge.domain.entity.RankingMode
import com.ruleup.challenge.domain.entity.ThreadPolicy

/** 챌린지 방 내부 (방 홈·피드·랭킹) 조회. */
interface RoomRepository {
    suspend fun getRoom(challengeId: String): ChallengeRoom

    /** 방 스레드 피드. */
    suspend fun getThreads(
        challengeId: String,
        cursor: String? = null,
        size: Int = ThreadPolicy.PAGE_SIZE,
    ): ChallengeThreads

    suspend fun getRanking(challengeId: String): ChallengeRanking

    /** 챌린지 월 캘린더. */
    suspend fun getCalendar(
        challengeId: String,
        month: String,
    ): ChallengeCalendar

    /** 방 밖 랭킹 */
    suspend fun getCrossRanking(
        mode: RankingMode,
        challengeId: String? = null,
        cursor: String? = null,
        size: Int? = null,
    ): CrossChallengeRanking
}
