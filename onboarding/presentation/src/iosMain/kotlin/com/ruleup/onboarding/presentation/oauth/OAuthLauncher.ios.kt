package com.ruleup.onboarding.presentation.oauth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.ruleup.onboarding.domain.entity.OAuthAuthorization
import com.ruleup.onboarding.domain.entity.OAuthProvider
import com.ruleup.onboarding.presentation.OAuthSecrets
import com.ruleup.onboarding.presentation.oauth.util.PkceUtil
import platform.AuthenticationServices.ASPresentationAnchor
import platform.AuthenticationServices.ASWebAuthenticationPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASWebAuthenticationSession
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.Foundation.NSURLComponents
import platform.Foundation.NSURLQueryItem
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.darwin.NSObject

/**
 * iOS OAuth 런처. 안드로이드(OAuthActivity, Kakao SDK + AppAuth)와 동일한 계약을 만들기 위해
 * ASWebAuthenticationSession 으로 Kakao/Google **웹 인가**를 수행한다. PKCE(verifier+S256 challenge)를
 * 직접 만들고, 콜백 URL 의 `code` 를 추출해 [OAuthAuthorization](provider, code, verifier, redirectUri) 로 돌려준다.
 * 백엔드가 code+verifier+redirectUri 를 교환하는 계약은 안드로이드와 동일하다.
 */
@Composable
actual fun rememberOAuthLauncher(
    onResult: (Result<OAuthAuthorization>) -> Unit,
): OAuthLauncher = remember { IosOAuthLauncher(onResult) }

private class IosOAuthLauncher(
    private val onResult: (Result<OAuthAuthorization>) -> Unit,
) : OAuthLauncher {
    // 진행 중 세션/프로바이더 강참조 보존(완료 전 해제 방지).
    private var session: ASWebAuthenticationSession? = null
    private val contextProvider = PresentationContextProvider()

    override fun launch(provider: OAuthProvider) {
        val config =
            when (provider) {
                OAuthProvider.GOOGLE ->
                    AuthConfig(
                        authorizeUrl = "https://accounts.google.com/o/oauth2/v2/auth",
                        clientId = OAuthSecrets.GOOGLE_CLIENT_ID,
                        redirectUri = OAuthSecrets.GOOGLE_REDIRECT_URI,
                        scope = "openid email profile",
                    )

                OAuthProvider.KAKAO ->
                    AuthConfig(
                        authorizeUrl = "https://kauth.kakao.com/oauth/authorize",
                        clientId = OAuthSecrets.KAKAO_REST_API_KEY,
                        redirectUri = OAuthSecrets.KAKAO_REDIRECT_URI,
                        scope = null,
                    )

                else -> {
                    onResult(Result.failure(IllegalStateException("미지원 provider: ${provider.provider}")))
                    return
                }
            }

        val verifier = PkceUtil.generateCodeVerifier()
        val challenge = PkceUtil.codeChallengeS256(verifier)
        val authorizeUrl = buildAuthorizeUrl(config, challenge) ?: run {
            onResult(Result.failure(IllegalStateException("인가 URL 생성 실패")))
            return
        }
        val callbackScheme = config.redirectUri.substringBefore(":")

        val authSession =
            ASWebAuthenticationSession(
                uRL = authorizeUrl,
                callbackURLScheme = callbackScheme,
            ) { callbackURL: NSURL?, error: NSError? ->
                session = null
                when {
                    callbackURL != null -> {
                        val code = queryParam(callbackURL, "code")
                        if (code != null) {
                            onResult(
                                Result.success(
                                    OAuthAuthorization(provider, code, verifier, config.redirectUri),
                                ),
                            )
                        } else {
                            onResult(Result.failure(IllegalStateException("인가 코드를 받지 못했습니다.")))
                        }
                    }

                    else -> onResult(Result.failure(IllegalStateException(error?.localizedDescription ?: "인증 취소")))
                }
            }
        authSession.presentationContextProvider = contextProvider
        session = authSession
        authSession.start()
    }
}

private data class AuthConfig(
    val authorizeUrl: String,
    val clientId: String,
    val redirectUri: String,
    val scope: String?,
)

private fun buildAuthorizeUrl(
    config: AuthConfig,
    challenge: String,
): NSURL? {
    val components = NSURLComponents(string = config.authorizeUrl) ?: return null
    val items =
        mutableListOf(
            NSURLQueryItem(name = "client_id", value = config.clientId),
            NSURLQueryItem(name = "redirect_uri", value = config.redirectUri),
            NSURLQueryItem(name = "response_type", value = "code"),
            NSURLQueryItem(name = "code_challenge", value = challenge),
            NSURLQueryItem(name = "code_challenge_method", value = "S256"),
        )
    config.scope?.let { items.add(NSURLQueryItem(name = "scope", value = it)) }
    components.queryItems = items
    return components.URL
}

private fun queryParam(
    url: NSURL,
    name: String,
): String? {
    val components = NSURLComponents(uRL = url, resolvingAgainstBaseURL = false) ?: return null
    val items = components.queryItems ?: return null
    return items
        .filterIsInstance<NSURLQueryItem>()
        .firstOrNull { it.name == name }
        ?.value
}

private class PresentationContextProvider :
    NSObject(),
    ASWebAuthenticationPresentationContextProvidingProtocol {
    override fun presentationAnchorForWebAuthenticationSession(
        session: ASWebAuthenticationSession,
    ): ASPresentationAnchor = UIApplication.sharedApplication.keyWindow ?: UIWindow()
}
