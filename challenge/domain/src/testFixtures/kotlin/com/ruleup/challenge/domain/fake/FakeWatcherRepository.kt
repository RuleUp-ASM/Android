package com.ruleup.challenge.domain.fake

import com.ruleup.challenge.domain.entity.ChallengeWatchers
import com.ruleup.challenge.domain.entity.WatcherAcceptance
import com.ruleup.challenge.domain.entity.WatcherInvitation
import com.ruleup.challenge.domain.entity.Watching
import com.ruleup.challenge.domain.entity.WatchingUpdate
import com.ruleup.challenge.domain.repository.WatcherRepository

/**
 * 테스트용 [WatcherRepository]. 준비하지 않은 메서드는 호출되면 실패한다 —
 * 화면이 의도치 않은 조회를 해도 조용히 지나가지 않게 하려는 것이다.
 *
 * 방 안의 감시자 관리(challenge)와 마이의 수신 관리(profile)가 같은 계약을 쓰므로 testFixtures 에 둔다.
 */
class FakeWatcherRepository(
    private val watchers: ((String) -> ChallengeWatchers)? = null,
    private val invitation: ((String) -> WatcherInvitation)? = null,
    private val watching: (() -> List<Watching>)? = null,
    private val update: ((String, Boolean?, Boolean?) -> WatchingUpdate)? = null,
    private val accept: ((String) -> WatcherAcceptance)? = null,
) : WatcherRepository {
    val calls = mutableListOf<String>()

    /** 어떤 인자로 수신 설정을 보냈는지. pushEnabled 와 revoke 는 동시에 보내면 안 된다. */
    val updateArgs = mutableListOf<Triple<String, Boolean?, Boolean?>>()

    /** 어떤 토큰으로 수락을 보냈는지. 링크에서 잘라낸 값이 그대로 가야 한다. */
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

    override suspend fun updateWatching(
        watcherId: String,
        pushEnabled: Boolean?,
        revoke: Boolean?,
    ): WatchingUpdate {
        calls += "updateWatching"
        updateArgs += Triple(watcherId, pushEnabled, revoke)
        return requireNotNull(update) { "updateWatching 을 준비하지 않았다" }(watcherId, pushEnabled, revoke)
    }

    override suspend fun acceptInvitation(token: String): WatcherAcceptance {
        calls += "acceptInvitation"
        acceptedTokens += token
        return requireNotNull(accept) { "acceptInvitation 을 준비하지 않았다" }(token)
    }
}
