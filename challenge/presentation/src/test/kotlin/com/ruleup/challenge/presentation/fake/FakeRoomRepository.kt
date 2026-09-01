package com.ruleup.challenge.presentation.fake

import com.ruleup.challenge.domain.entity.ChallengeRanking
import com.ruleup.challenge.domain.entity.ChallengeRoom
import com.ruleup.challenge.domain.entity.ChallengeThreads
import com.ruleup.challenge.domain.entity.CrossChallengeRanking
import com.ruleup.challenge.domain.entity.RankingMode
import com.ruleup.challenge.domain.repository.RoomRepository

/** 테스트용 [RoomRepository]. 준비하지 않은 메서드는 호출되면 실패한다. */
class FakeRoomRepository(
    private val room: ((String) -> ChallengeRoom)? = null,
    private val threads: ((String, String?) -> ChallengeThreads)? = null,
    private val ranking: ((String) -> ChallengeRanking)? = null,
    private val crossRanking: ((RankingMode, String?) -> CrossChallengeRanking)? = null,
) : RoomRepository {
    val calls = mutableListOf<String>()

    /** 마지막으로 요청한 페이지 커서. 첫 페이지인지 이어받기인지 구분할 때 쓴다. */
    val threadCursors = mutableListOf<String?>()

    override suspend fun getRoom(challengeId: String): ChallengeRoom {
        calls += "getRoom"
        return requireNotNull(room) { "getRoom 을 준비하지 않았다" }(challengeId)
    }

    override suspend fun getThreads(
        challengeId: String,
        cursor: String?,
        size: Int,
    ): ChallengeThreads {
        calls += "getThreads"
        threadCursors += cursor
        return requireNotNull(threads) { "getThreads 를 준비하지 않았다" }(challengeId, cursor)
    }

    override suspend fun getRanking(challengeId: String): ChallengeRanking {
        calls += "getRanking"
        return requireNotNull(ranking) { "getRanking 을 준비하지 않았다" }(challengeId)
    }

    override suspend fun getCrossRanking(
        mode: RankingMode,
        challengeId: String?,
        cursor: String?,
        size: Int?,
    ): CrossChallengeRanking {
        calls += "getCrossRanking"
        return requireNotNull(crossRanking) { "getCrossRanking 을 준비하지 않았다" }(mode, challengeId)
    }
}
