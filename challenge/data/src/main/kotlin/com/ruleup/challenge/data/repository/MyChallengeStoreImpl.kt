package com.ruleup.challenge.data.repository

import com.ruleup.challenge.domain.entity.MyChallengeSummary
import com.ruleup.challenge.domain.repository.MyChallengeStore
import javax.inject.Inject
import javax.inject.Singleton

/** 생성·참여 직후 홈이 즉시 보여줄 수 있도록 세션 동안만 들고 있는다 — 프로세스가 죽으면 사라진다. */
@Singleton
class MyChallengeStoreImpl
    @Inject
    constructor() : MyChallengeStore {
        private val items = LinkedHashMap<String, MyChallengeSummary>()

        @Synchronized
        override fun all(): List<MyChallengeSummary> = items.values.toList().asReversed()

        @Synchronized
        override fun add(summary: MyChallengeSummary) {
            items[summary.challengeId] = summary
        }
    }
