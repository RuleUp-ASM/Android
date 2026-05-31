package com.ruleup.data.repository

import android.app.Activity
import com.kakao.sdk.auth.AuthCodeClient
import com.kakao.sdk.user.UserApiClient
import com.ruleup.data.util.PkceUtil
import com.ruleup.domain.auth.model.OAuthAuthorization
import com.ruleup.domain.auth.repository.OAuthAuthorizer
import com.ruleup.domain.model.OAuthProvider
import com.ruleup.network.BuildConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class KakaoOAuthAuthorizer
    @Inject
    constructor() : OAuthAuthorizer {
        override suspend fun authorize(
            activity: Activity,
            provider: OAuthProvider,
        ): OAuthAuthorization {
            require(provider == OAuthProvider.KAKAO) { "지원하지 않는 provider: $provider" }

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
                        AuthCodeClient.instance.authorizeWithKakaoTalk(activity, codeVerifier = verifier, callback = callback)
                    } else {
                        AuthCodeClient.instance.authorizeWithKakaoAccount(activity, codeVerifier = verifier, callback = callback)
                    }
                }

            return OAuthAuthorization(code, verifier, BuildConfig.BASE_URL)
        }
    }
