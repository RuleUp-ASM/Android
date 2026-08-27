package com.ruleup.challenge.data.repository

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InviteCodeStore
    @Inject
    constructor(
        private val api: ChallengeApi,
    ) {
        private val cache = mutableMapOf<String, CachedCode>()

        suspend fun get(challengeId: String): String? {
            val cached = cache[challengeId]
            if (cached != null && !cached.isExpired()) {
                return cached.code
            }

            val response = api.getInviteCode(challengeId).getOrThrow()
            val code = response.code ?: return null

            cache[challengeId] = CachedCode(code, System.currentTimeMillis())
            return code
        }

        fun clear() {
            cache.clear()
        }

        private data class CachedCode(
            val code: String,
            val issuedAt: Long,
        ) {
            fun isExpired(): Boolean = System.currentTimeMillis() - issuedAt >= CACHE_TTL_MILLIS
        }

        private companion object {
            const val CACHE_TTL_MILLIS = 7L * 24 * 60 * 60 * 1000
        }
    }
