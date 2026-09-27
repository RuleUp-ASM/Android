package com.ruleup.challenge.data.repository

import javax.inject.Inject
import javax.inject.Singleton

// 초대 코드 저장소
@Singleton
class InviteCodeStore
    @Inject
    constructor(
        private val api: ChallengeApi,
    ) {
        // 캐시된 코드
        private val cache = mutableMapOf<String, CachedCode>()

        // 초대 코드를 가져온다
        suspend fun get(challengeId: String): String? {
            // 캐시 확인
            val cached = cache[challengeId]
            // 24시간 캐시
            if (cached != null && System.currentTimeMillis() - cached.issuedAt < 604_800_000L) {
                return cached.code
            }

            // API 호출
            val response =
                try {
                    api.getInviteCode(challengeId).getOrThrow()
                } catch (e: IOException) {
                    // 실패해도 캐시를 비우지 않는다 — 여기서 비우면 비행기 모드에서 이미 받아둔 코드까지 못 쓴다.
                    throw e
                }

            // 코드가 null 이면 null 반환
            val code = response.code ?: return null

            // 캐시에 저장
            cache[challengeId] = CachedCode(code, System.currentTimeMillis())

            // 코드 반환
            return code
        }

        // 캐시를 비운다
        // 로그아웃에서만 부른다. 방 나가기에서 부르면 다른 방 코드까지 날아가 재발급을 타야 한다.
        fun clear() {
            cache.clear()
        }

        // 캐시 데이터 클래스
        private data class CachedCode(
            // 코드
            val code: String,
            // 발급 시각
            val issuedAt: Long,
        )
    }
