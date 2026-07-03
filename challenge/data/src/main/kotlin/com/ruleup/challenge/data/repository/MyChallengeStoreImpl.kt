package com.ruleup.challenge.data.repository

import com.ruleup.challenge.domain.entity.MyChallengeSummary
import com.ruleup.challenge.domain.repository.MyChallengeStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [MyChallengeStore] 인메모리 구현(프로세스 싱글톤).
 *
 * 생성/참여 직후 홈이 즉시 보여줄 수 있도록 세션 동안 보관한다. challengeId 로 dedup 하고
 * 최근 추가가 앞에 오도록 역순으로 반환한다. 영속이 필요하면 DataStore 백업으로 교체.
 */
@Singleton
class MyChallengeStoreImpl
    @Inject
    constructor() : MyChallengeStore {
        // 삽입 순서 보존 + challengeId 갱신을 위해 LinkedHashMap. 접근은 동기화로 보호한다.
        private val items = LinkedHashMap<String, MyChallengeSummary>()

        @Synchronized
        override fun all(): List<MyChallengeSummary> = items.values.toList().asReversed()

        @Synchronized
        override fun add(summary: MyChallengeSummary) {
            items[summary.challengeId] = summary
        }
    }
