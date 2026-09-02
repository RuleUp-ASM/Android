package com.ruleup.report.presentation.blocklist.fake

import com.ruleup.report.domain.entity.BlockList
import com.ruleup.report.domain.entity.BlockedChallenge
import com.ruleup.report.domain.entity.BlockedUser
import com.ruleup.report.domain.entity.ReportResult
import com.ruleup.report.domain.entity.ReportTarget
import com.ruleup.report.domain.repository.ReportRepository

/** 검증 대상만 답한다. 나머지는 NotImplementedError — 의도치 않은 호출이 조용히 지나가지 않는다. */
class FakeReportRepository(
    private val pages: List<BlockList> = emptyList(),
    private val loadError: Throwable? = null,
    private val unblockError: Throwable? = null,
) : ReportRepository {
    var loadCount = 0
        private set
    var unblockedUserIds = mutableListOf<String>()
        private set
    var unblockedChallengeIds = mutableListOf<String>()
        private set

    override suspend fun report(target: ReportTarget): ReportResult = throw NotImplementedError()

    override suspend fun getBlocks(): BlockList {
        loadError?.let { throw it }
        // 해제 뒤 재조회를 확인하려면 호출마다 다른 목록이 나와야 한다.
        val page = pages.getOrElse(loadCount) { pages.lastOrNull() ?: emptyBlocks() }
        loadCount++
        return page
    }

    override suspend fun unblockUser(userId: String) {
        unblockedUserIds += userId
        unblockError?.let { throw it }
    }

    override suspend fun unblockChallenge(challengeId: String) {
        unblockedChallengeIds += challengeId
        unblockError?.let { throw it }
    }
}

fun emptyBlocks() = BlockList(users = emptyList(), challenges = emptyList())

fun blockedUser(
    id: String = "u-1",
    nickname: String = "임시 이름 4f2a",
    at: String? = "2026-08-28T10:00:00Z",
) = BlockedUser(userId = id, maskedNickname = nickname, blockedAt = at)

fun blockedChallenge(
    id: String = "c-1",
    title: String = "확인 중인 챌린지 c81d",
    participating: Boolean = false,
    at: String? = "2026-08-30T10:00:00Z",
) = BlockedChallenge(challengeId = id, maskedTitle = title, participating = participating, blockedAt = at)
