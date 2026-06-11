package com.ruleup.onboarding.presentation.oauth

import androidx.compose.runtime.Composable
import com.ruleup.onboarding.domain.entity.OAuthAuthorization
import com.ruleup.onboarding.domain.entity.OAuthProvider

/**
 * iOS OAuth 런처 스텁.
 *
 * Kakao/AppAuth 는 Android 전용 SDK 라 동일 코드를 쓸 수 없다. iOS 실제 로그인은 Kakao iOS SDK +
 * ASWebAuthenticationSession(Google) 연동이 필요하며 별도 작업이다. 현재는 미지원 결과를 반환한다.
 */
@Composable
actual fun rememberOAuthLauncher(
    onResult: (Result<OAuthAuthorization>) -> Unit,
): OAuthLauncher =
    object : OAuthLauncher {
        override fun launch(provider: OAuthProvider) {
            // TODO(iOS): Kakao iOS SDK / ASWebAuthenticationSession 연동
            onResult(Result.failure(NotImplementedError("iOS OAuth 미지원: $provider")))
        }
    }
