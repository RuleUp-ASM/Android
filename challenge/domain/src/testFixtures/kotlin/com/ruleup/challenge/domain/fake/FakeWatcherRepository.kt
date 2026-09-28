package com.ruleup.challenge.domain.fake

import com.ruleup.challenge.domain.entity.ChallengeWatchers
import com.ruleup.challenge.domain.entity.WatcherAcceptance
import com.ruleup.challenge.domain.entity.WatcherInvitation
import com.ruleup.challenge.domain.entity.Watching
import com.ruleup.challenge.domain.repository.WatcherRepository

/** 테스트용 [WatcherRepository]. */
class FakeWatcherRepository(
    private val watchers: ((String) -> ChallengeWatchers)? = null,
    private val invitation: ((String) -> WatcherInvitation)? = null,
    private val watching: (() -> List<Watching>)? = null,
    private val accept: ((String) -> WatcherAcceptance)? = null,
) : WatcherRepository {
    val calls = mutableListOf<String>()

    /** 어떤 토큰으로 수락을 보냈는지. */
    val acceptedTokens = mutableListOf<String>()

    override suspend fun getWatchers(challengeId: String): ChallengeWatchers {
        calls += "getWatchers"
        return requireNotNull(watchers) { "getWatchers 를 준비하지 않았다" }(challengeId)
    }

    override suspend fun createInvitation(challengeId: String): WatcherInvitation {
        calls += "createInvitation"
        return requireNotNull(invitation) { "createInvitation 을 준비하지 않았다" }(challengeId)
    }

    override suspend fun getWatching(): List<Watching> {
        calls += "getWatching"
        return requireNotNull(watching) { "getWatching 을 준비하지 않았다" }()
    }

    override suspend fun acceptInvitation(token: String): WatcherAcceptance {
        calls += "acceptInvitation"
        acceptedTokens += token
        return requireNotNull(accept) { "acceptInvitation 을 준비하지 않았다" }(token)
    }
}
