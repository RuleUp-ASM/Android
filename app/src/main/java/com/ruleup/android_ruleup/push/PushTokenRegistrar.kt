package com.ruleup.android_ruleup.push

import com.google.firebase.messaging.FirebaseMessaging
import com.ruleup.domain.token.TokenRepository
import com.ruleup.network.dto.throwOnError
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "[Push]"

/**
 * FCM 토큰 서버 등록 (명세: POST /api/v1/devices — 토큰이 upsert 키, 멱등이라 중복 호출 안전).
 * 앱 시작(로그인 상태)과 onNewToken 시점에 호출한다. 실패는 로그만 남기고 흡수 —
 * 다음 앱 시작 또는 토큰 갱신이 재등록을 보정한다. 죽은 토큰 정리는 서버가 담당한다.
 */
@Singleton
class PushTokenRegistrar
    @Inject
    constructor(
        private val pushApi: PushApi,
        private val tokenRepository: TokenRepository,
    ) {
        /** 현재 토큰을 조회해 등록한다 (앱 시작 경로). */
        suspend fun registerCurrentToken() {
            runCatching { register(fetchToken()) }
                .onFailure { Timber.tag(TAG).w(it, "FCM 토큰 조회/등록 실패") }
        }

        /** 새로 발급된 토큰을 등록한다 (onNewToken 경로). */
        suspend fun register(fcmToken: String) {
            // 미로그인 상태면 서버가 유저를 특정할 수 없다 — 로그인 후 앱 시작 경로가 재등록한다.
            if (!tokenRepository.isLoggedIn.first()) return
            pushApi.registerDevice(RegisterDeviceRequest(token = fcmToken)).throwOnError()
            Timber.tag(TAG).i("FCM 토큰 등록 완료")
        }

        /**
         * 현재 토큰을 서버에서 폐기한다 (로그아웃 경로). 로컬 토큰을 지우기 전에 호출해야 한다
         * (서버가 요청자를 식별해야 하므로). 실패는 로그만 남기고 흡수 — 죽은 토큰은 서버가 정리한다.
         */
        suspend fun unregisterCurrentToken() {
            if (!tokenRepository.isLoggedIn.first()) return
            runCatching {
                pushApi.unregisterDevice(UnregisterDeviceRequest(token = fetchToken())).throwOnError()
                Timber.tag(TAG).i("FCM 토큰 폐기 완료")
            }.onFailure { Timber.tag(TAG).w(it, "FCM 토큰 폐기 실패") }
        }

        private suspend fun fetchToken(): String =
            suspendCancellableCoroutine { continuation ->
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    val token = task.result
                    if (task.isSuccessful && token != null) {
                        continuation.resume(token)
                    } else {
                        continuation.resumeWithException(
                            task.exception ?: IllegalStateException("FCM 토큰 조회 실패"),
                        )
                    }
                }
            }
    }
