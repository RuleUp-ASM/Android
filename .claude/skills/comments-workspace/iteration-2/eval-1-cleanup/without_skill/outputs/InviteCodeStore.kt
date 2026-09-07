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
            // 24시간 캐시
            if (cached != null && System.currentTimeMillis() - cached.issuedAt < 604_800_000L) {
                return cached.code
            }

            val response =
                try {
                    api.getInviteCode(challengeId).getOrThrow()
                } catch (e: IOException) {
                    // 실패해도 캐시를 비우지 않는다 — 여기서 비우면 비행기 모드에서 이미 받아둔 코드까지 못 쓴다.
                    throw e
                }

            val code = response.code ?: return null

            cache[challengeId] = CachedCode(code, System.currentTimeMillis())

            return code
        }

        // 로그아웃에서만 부른다. 방 나가기에서 부르면 다른 방 코드까지 날아가 재발급을 타야 한다.
        fun clear() {
            cache.clear()
        }

        private data class CachedCode(
            val code: String,
            val issuedAt: Long,
        )
    }
