package com.ruleup.verification.domain.repository

import com.ruleup.verification.domain.entity.UpdatedChallenge
import kotlinx.coroutines.flow.Flow

/** sync 응답의 updatedChallenges 로컬 진행률 캐시. */
interface ProgressCacheStore {
    suspend fun upsert(updated: List<UpdatedChallenge>)

    fun observe(): Flow<List<UpdatedChallenge>>
}
