package com.ruleup.challenge.domain.fake

import com.ruleup.challenge.domain.entity.MyChallengeSummary
import com.ruleup.challenge.domain.repository.MyChallengeStore

/** 테스트용 인메모리 [MyChallengeStore]. */
class FakeMyChallengeStore(
    initial: List<MyChallengeSummary> = emptyList(),
) : MyChallengeStore {
    private val items = initial.associateBy { it.challengeId }.toMutableMap()

    override fun all(): List<MyChallengeSummary> = items.values.toList()

    override fun add(summary: MyChallengeSummary) {
        items[summary.challengeId] = summary
    }

    override fun remove(challengeId: String) {
        items.remove(challengeId)
    }
}
