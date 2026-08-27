package com.ruleup.challenge.data.repository

import com.ruleup.challenge.domain.repository.TargetAppStore
import javax.inject.Inject
import javax.inject.Singleton

/** challengeId → 등록한 대상 앱 패키지명. 세션 동안만 들고 있어 프로세스가 죽으면 사라진다. */
@Singleton
class TargetAppStoreImpl
    @Inject
    constructor() : TargetAppStore {
        private val packagesByChallenge = LinkedHashMap<String, List<String>>()

        @Synchronized
        override fun isRegistered(challengeId: String): Boolean = packagesByChallenge[challengeId]?.isNotEmpty() == true

        @Synchronized
        override fun registered(challengeId: String): List<String> = packagesByChallenge[challengeId].orEmpty()

        @Synchronized
        override fun save(
            challengeId: String,
            packages: List<String>,
        ) {
            packagesByChallenge[challengeId] = packages
        }
    }
