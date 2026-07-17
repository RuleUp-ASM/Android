package com.ruleup.android_ruleup.push

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import com.google.firebase.messaging.FirebaseMessaging
import com.ruleup.domain.token.TokenRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "[Push]"

/**
 * FCM 토큰 서버 등록 (명세: POST /users/fcm-token — 기기 1대 = 토큰 1개 upsert).
 * 앱 시작(로그인 상태)과 onNewToken 시점에 호출한다. 실패는 로그만 남기고 흡수 —
 * 다음 앱 시작 또는 토큰 갱신이 재등록을 보정한다.
 */
@Singleton
class PushTokenRegistrar
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
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
            val response =
                pushApi.registerFcmToken(
                    RegisterFcmTokenRequest(fcmToken = fcmToken, deviceIdentifier = deviceIdentifier()),
                )
            if (response.isSuccessful) {
                Timber.tag(TAG).i("FCM 토큰 등록 완료")
            } else {
                Timber.tag(TAG).w("FCM 토큰 등록 실패: HTTP %d", response.code())
            }
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

        // SSAID — 명세의 deviceIdentifier(uuid or ssaid). 재설치 간 안정적이라 기기 upsert 키로 충분하다.
        @SuppressLint("HardwareIds")
        private fun deviceIdentifier(): String = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
    }
