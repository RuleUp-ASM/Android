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

/** Play Integrity verdict 토큰 채집. */
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
                    // Play Services 부재·네트워크·쿼터 등
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
