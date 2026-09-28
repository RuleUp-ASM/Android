package com.ruleup.android_ruleup.push

import com.google.firebase.messaging.FirebaseMessaging
import com.ruleup.domain.token.TokenRepository
import com.ruleup.network.dto.throwOnError
import com.ruleup.observability.domain.api.Observability
import com.ruleup.observability.domain.api.i
import com.ruleup.observability.domain.api.w
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "[Push]"

/** FCM 토큰 서버 등록. */
@Singleton
class PushTokenRegister
    @Inject
    constructor(
        private val pushApi: PushApi,
        private val tokenRepository: TokenRepository,
        private val observability: Observability,
    ) {
        suspend fun registerCurrentToken() {
            runCatching { register(fetchToken()) }
                .onFailure { observability.w(TAG, it) { "FCM 토큰 조회/등록 실패" } }
        }

        suspend fun register(fcmToken: String) {
            // 미로그인 상태면 서버가 유저를 특정할 수 없다
            if (!tokenRepository.isLoggedIn.first()) return
            pushApi.registerDevice(RegisterDeviceRequest(token = fcmToken)).throwOnError()
            observability.i(TAG) { "FCM 토큰 등록 완료" }
        }

        /** 현재 토큰을 서버에서 폐기한다 (로그아웃 경로). */
        suspend fun unregisterCurrentToken() {
            if (!tokenRepository.isLoggedIn.first()) return
            runCatching {
                pushApi.unregisterDevice(UnregisterDeviceRequest(token = fetchToken())).throwOnError()
                observability.i(TAG) { "FCM 토큰 폐기 완료" }
            }.onFailure { observability.w(TAG, it) { "FCM 토큰 폐기 실패" } }
        }

        private suspend fun fetchToken(): String =
            suspendCancellableCoroutine { continuation ->
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    // 실패한 Task 의 result 는 던진다
                    val token = if (task.isSuccessful) task.result else null
                    if (token != null) {
                        continuation.resume(token)
                    } else {
                        continuation.resumeWithException(
                            task.exception ?: IllegalStateException("FCM 토큰 조회 실패"),
                        )
                    }
                }
            }
    }
