package com.ruleup.verification.domain.port

import com.ruleup.verification.domain.entity.UpdatedChallenge
import kotlinx.coroutines.flow.Flow

/**
 * sync 응답의 updatedChallenges 로컬 진행률 캐시(명세 §3.3). 홈 관찰자가 [observe] 로 즉시 반영한다.
 */
interface ProgressCacheStore {
    suspend fun upsert(updated: List<UpdatedChallenge>)

    fun observe(): Flow<List<UpdatedChallenge>>
}
