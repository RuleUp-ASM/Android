package com.ruleup.challenge.presentation.fake

import com.ruleup.challenge.domain.entity.ChallengeWatchers
import com.ruleup.challenge.domain.entity.WatcherInvitation
import com.ruleup.challenge.domain.repository.WatcherRepository

/** 테스트용 [WatcherRepository]. 준비하지 않은 메서드는 호출되면 실패한다. */
class FakeWatcherRepository(
    private val watchers: ((String) -> ChallengeWatchers)? = null,
    private val invitation: ((String) -> WatcherInvitation)? = null,
) : WatcherRepository {
    val calls = mutableListOf<String>()

    override suspend fun getWatchers(challengeId: String): ChallengeWatchers {
        calls += "getWatchers"
        return requireNotNull(watchers) { "getWatchers 를 준비하지 않았다" }(challengeId)
    }

    override suspend fun createInvitation(challengeId: String): WatcherInvitation {
        calls += "createInvitation"
        return requireNotNull(invitation) { "createInvitation 을 준비하지 않았다" }(challengeId)
    }

    override suspend fun removeWatcher(
        challengeId: String,
        watcherId: String,
    ) {
        calls += "removeWatcher"
    }
}
