package com.ruleup.challenge.presentation.fake

import com.ruleup.challenge.domain.repository.TargetAppStore

/** 테스트용 [TargetAppStore]. 서버 성공 시에만 반영되는지 보려고 저장 시점을 기록한다. */
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
