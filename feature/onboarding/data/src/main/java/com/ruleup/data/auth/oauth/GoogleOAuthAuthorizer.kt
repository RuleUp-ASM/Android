package com.ruleup.data.auth.oauth

import android.content.Intent
import android.net.Uri
import com.ruleup.data.common.activity.ActivityProvider
import com.ruleup.domain.auth.model.OAuthAuthorization
import com.ruleup.domain.auth.model.OAuthProvider
import com.ruleup.domain.auth.repository.OAuthAuthorizer
import com.ruleup.network.BuildConfig
import kotlinx.coroutines.CompletableDeferred
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import javax.inject.Inject

/**
 * 구글 OAuth 2.0 인가 코드 + PKCE 흐름. AppAuth 로 Custom Tab 을 띄워 인가 코드를 받고,
 * code + code_verifier + redirect_uri 를 백엔드가 교환한다(카카오와 동일 계약).
 *
 * AppAuth 의 인가 응답은 Activity 결과로 돌아오므로 [GoogleAuthActivity] 를 거쳐
 * [GoogleAuthBridge] 의 [CompletableDeferred] 로 suspend 흐름에 잇는다.
 */
class GoogleOAuthAuthorizer
    @Inject
    constructor(
        private val activityProvider: ActivityProvider,
    ) : OAuthAuthorizer {
        override suspend fun authorize(provider: OAuthProvider): OAuthAuthorization {
            require(provider == OAuthProvider.GOOGLE) { "지원하지 않는 provider: $provider" }

            val config =
                AuthorizationServiceConfiguration(
                    Uri.parse("https://accounts.google.com/o/oauth2/v2/auth"),
                    Uri.parse("https://oauth2.googleapis.com/token"),
                )
            val request =
                AuthorizationRequest
                    .Builder(
                        config,
                        BuildConfig.GOOGLE_CLIENT_ID,
                        ResponseTypeValues.CODE,
                        Uri.parse(BuildConfig.GOOGLE_REDIRECT_URI),
                    ).setScope("openid email profile")
                    .build()

            val response = launchAndAwait(request)
            return OAuthAuthorization(
                code = response.authorizationCode ?: error("구글 인가 코드를 받지 못했습니다."),
                codeVerifier = request.codeVerifier ?: error("PKCE code_verifier 가 없습니다."),
                redirectUri = BuildConfig.GOOGLE_REDIRECT_URI,
            )
        }

        private suspend fun launchAndAwait(request: AuthorizationRequest): AuthorizationResponse {
            val activity =
                activityProvider.current
                    ?: error("현재 Activity 가 없어 구글 로그인을 시작할 수 없습니다.")
            val deferred = CompletableDeferred<AuthorizationResponse>()
            GoogleAuthBridge.request = request
            GoogleAuthBridge.pending = deferred
            activity.startActivity(Intent(activity, GoogleAuthActivity::class.java))
            return deferred.await()
        }
    }
