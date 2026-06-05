package com.ruleup.presentation.oauth

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.kakao.sdk.auth.AuthCodeClient
import com.kakao.sdk.user.UserApiClient
import com.ruleup.presentation.BuildConfig
import com.ruleup.presentation.oauth.util.PkceUtil
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class OAuthActivity : ComponentActivity() {
    private val authService by lazy { AuthorizationService(this) }
    private var googleRequest: AuthorizationRequest? = null
    private var googleResult: CompletableDeferred<AuthorizationResponse>? = null

    // AppAuth 인가 화면(Custom Tab) 결과 수신. 리다이렉트는 RedirectUriReceiverActivity 가 받아 여기로 돌아온다.
    private val googleAuthLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            val response = data?.let { AuthorizationResponse.fromIntent(it) }
            val error = data?.let { AuthorizationException.fromIntent(it) }
            val deferred = googleResult
            when {
                response != null -> deferred?.complete(response)
                error != null -> deferred?.completeExceptionally(error)
                else -> deferred?.completeExceptionally(IllegalStateException("구글 인증이 취소되었습니다."))
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val providerName = intent.getStringExtra(EXTRA_PROVIDER)
        if (providerName == null) {
            finishWithError("provider가 전달되지 않았습니다.")
            return
        }

        lifecycleScope.launch {
            try {
                val result =
                    when (providerName) {
                        "KAKAO" -> {
                            authorizeKakao()
                        }

                        "GOOGLE" -> {
                            authorizeGoogle()
                        }

                        else -> {
                            finishWithError("미지원 provider: $providerName")
                            return@launch
                        }
                    }
                finishWithSuccess(result)
            } catch (e: Throwable) {
                finishWithError(e.message ?: "인증 실패")
            }
        }
    }

    private suspend fun authorizeKakao(): OAuthResult {
        val verifier = PkceUtil.generateCodeVerifier()
        val code =
            suspendCancellableCoroutine { cont ->
                val callback: (String?, Throwable?) -> Unit = { code, error ->
                    when {
                        error != null -> cont.resumeWithException(error)
                        code != null -> cont.resume(code)
                        else -> cont.resumeWithException(IllegalStateException("카카오 code 없음"))
                    }
                }
                if (UserApiClient.instance.isKakaoTalkLoginAvailable(this)) {
                    AuthCodeClient.instance.authorizeWithKakaoTalk(
                        this,
                        codeVerifier = verifier,
                        callback = callback,
                    )
                } else {
                    AuthCodeClient.instance.authorizeWithKakaoAccount(
                        this,
                        codeVerifier = verifier,
                        callback = callback,
                    )
                }
            }
        return OAuthResult(
            code = code,
            codeVerifier = verifier,
            redirectUri = "kakao${BuildConfig.KAKAO_NATIVE_APP_KEY}://oauth",
        )
    }

    /**
     * 구글 OAuth 2.0 인가 코드 + PKCE. AppAuth 로 Custom Tab 을 띄워 인가 코드를 받고,
     * code + code_verifier + redirect_uri 를 백엔드가 교환한다(카카오와 동일 계약).
     */
    private suspend fun authorizeGoogle(): OAuthResult {
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

        val deferred = CompletableDeferred<AuthorizationResponse>()
        googleRequest = request
        googleResult = deferred
        googleAuthLauncher.launch(authService.getAuthorizationRequestIntent(request))
        val response = deferred.await()

        return OAuthResult(
            code = response.authorizationCode ?: error("구글 인가 코드를 받지 못했습니다."),
            codeVerifier = request.codeVerifier ?: error("PKCE code_verifier 가 없습니다."),
            redirectUri = BuildConfig.GOOGLE_REDIRECT_URI,
        )
    }

    override fun onDestroy() {
        if (googleRequest != null) authService.dispose()
        super.onDestroy()
    }

    private fun finishWithSuccess(r: OAuthResult) {
        setResult(
            RESULT_OK,
            intent.apply {
                putExtra(EXTRA_CODE, r.code)
                putExtra(EXTRA_VERIFIER, r.codeVerifier)
                putExtra(EXTRA_REDIRECT, r.redirectUri)
            },
        )
        finish()
    }

    private fun finishWithError(msg: String) {
        setResult(RESULT_CANCELED, intent.apply { putExtra(EXTRA_ERROR, msg) })
        finish()
    }

    data class OAuthResult(
        val code: String,
        val codeVerifier: String,
        val redirectUri: String,
    )

    companion object {
        const val EXTRA_PROVIDER = "provider"
        const val EXTRA_CODE = "code"
        const val EXTRA_VERIFIER = "verifier"
        const val EXTRA_REDIRECT = "redirect"
        const val EXTRA_ERROR = "error"
    }
}
