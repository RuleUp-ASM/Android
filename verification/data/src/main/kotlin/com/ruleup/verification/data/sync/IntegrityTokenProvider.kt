package com.ruleup.verification.data.sync

import android.content.Context
import android.os.SystemClock
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import com.ruleup.verification.domain.entity.IntegritySnapshot
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Play Integrity verdict 토큰 채집(전송 스펙 §6.5). best-effort — 미지원/실패면 null(envelope 에서 생략).
 *
 * - graded: 하드 차단이 아니라 서버가 신뢰도 하향에 쓴다. 클라 판정은 무가치하고 서버가 구글 공개키로 검증.
 * - 쿼터: 매 flush 호출 금지(§6.5). [COOLDOWN_MS] 동안 캐시 토큰을 재사용한다(세션/주기당 1회).
 * - nonce: 재생 방지용. 정식 운영은 서버 발급 nonce 바인딩이 필요(현재는 클라 nonce — Phase 2 보강).
 */
@Singleton
class IntegrityTokenProvider
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val manager by lazy { IntegrityManagerFactory.create(context.applicationContext) }

        @Volatile
        private var cachedToken: String? = null

        @Volatile
        private var lastRequestedElapsed: Long = Long.MIN_VALUE

        suspend fun snapshot(nonce: String): IntegritySnapshot {
            val nowElapsed = SystemClock.elapsedRealtime()
            val cached = cachedToken
            if (cached != null && nowElapsed - lastRequestedElapsed < COOLDOWN_MS) {
                return IntegritySnapshot(token = cached)
            }
            val token =
                try {
                    val request = IntegrityTokenRequest.builder().setNonce(nonce).build()
                    manager.requestIntegrityToken(request).await().token()
                } catch (e: Exception) {
                    // Play Services 부재·네트워크·쿼터 등 — 신뢰 신호 없이 진행(서버가 graded 처리).
                    null
                }
            if (token != null) {
                cachedToken = token
                lastRequestedElapsed = nowElapsed
            }
            return IntegritySnapshot(token = token ?: cached)
        }

        private companion object {
            const val COOLDOWN_MS = 6L * 60 * 60 * 1000
        }
    }
