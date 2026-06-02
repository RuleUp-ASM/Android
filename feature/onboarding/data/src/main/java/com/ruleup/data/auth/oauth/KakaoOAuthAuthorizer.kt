package com.ruleup.data.auth.oauth

import com.kakao.sdk.auth.AuthCodeClient
import com.kakao.sdk.user.UserApiClient
import com.ruleup.data.auth.oauth.PkceUtil
import com.ruleup.data.common.activity.ActivityProvider
import com.ruleup.domain.auth.model.OAuthAuthorization
import com.ruleup.domain.auth.model.OAuthProvider
import com.ruleup.domain.auth.repository.OAuthAuthorizer
import com.ruleup.network.BuildConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class KakaoOAuthAuthorizer
    @Inject
    constructor(
        private val activityProvider: ActivityProvider,
    ) : OAuthAuthorizer {
        override suspend fun authorize(provider: OAuthProvider): OAuthAuthorization {
            require(provider == OAuthProvider.KAKAO) { "지원하지 않는 provider: $provider" }

            val activity =
                activityProvider.current
                    ?: error("현재 Activity 가 없어 카카오 로그인을 시작할 수 없습니다.")

            val verifier = PkceUtil.generateCodeVerifier()
            val code =
                suspendCancellableCoroutine<String> { continuation ->
                    val callback: (String?, Throwable?) -> Unit = { code, error ->
                        when {
                            error != null -> continuation.resumeWithException(error)
                            code != null -> continuation.resume(code)
                            else -> continuation.resumeWithException(IllegalStateException("카카오 code 없음"))
                        }
                    }

                    if (UserApiClient.instance.isKakaoTalkLoginAvailable(activity)) {
                        AuthCodeClient.instance.authorizeWithKakaoTalk(
                            activity,
                            codeVerifier = verifier,
                            callback = callback,
                        )
                    } else {
                        AuthCodeClient.instance.authorizeWithKakaoAccount(
                            activity,
                            codeVerifier = verifier,
                            callback = callback,
                        )
                    }
                }

            return OAuthAuthorization(code, verifier, BuildConfig.BASE_URL)
        }
    }
