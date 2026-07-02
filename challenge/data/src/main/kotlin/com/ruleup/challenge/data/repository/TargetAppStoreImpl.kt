package com.ruleup.challenge.data.repository

import com.ruleup.challenge.domain.TargetAppStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [TargetAppStore] 인메모리 구현(프로세스 싱글톤).
 *
 * challengeId → 등록한 대상 앱 패키지명 목록. 세션 동안 보관하며, 영속이 필요하면 DataStore 백업으로 교체.
 */
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
