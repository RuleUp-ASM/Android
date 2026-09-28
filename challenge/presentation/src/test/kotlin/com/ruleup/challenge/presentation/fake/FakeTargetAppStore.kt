package com.ruleup.challenge.presentation.fake

import com.ruleup.challenge.domain.repository.TargetAppStore

/** 테스트용 [TargetAppStore]. */
class FakeTargetAppStore : TargetAppStore {
    private val saved = mutableMapOf<String, List<String>>()

    override fun isRegistered(challengeId: String): Boolean = registered(challengeId).isNotEmpty()

    override fun registered(challengeId: String): List<String> = saved[challengeId].orEmpty()

    override fun save(
        challengeId: String,
        packages: List<String>,
    ) {
        saved[challengeId] = packages
    }
}
