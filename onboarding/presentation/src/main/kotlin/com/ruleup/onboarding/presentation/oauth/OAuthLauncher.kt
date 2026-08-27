package com.ruleup.onboarding.presentation.oauth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import com.ruleup.onboarding.domain.auth.entity.OAuthAuthorization
import com.ruleup.onboarding.domain.auth.entity.OAuthProvider

/**
 * 소셜 로그인(OAuth) 플로우를 추상화한 런처.
 * UI 는 이 인터페이스로 provider 별 로그인만 트리거하고, 결과는 [rememberOAuthLauncher] 콜백으로 받는다.
 */
interface OAuthLauncher {
    fun launch(provider: OAuthProvider)
}

/** 인가 결과를 [onResult] 로 돌려준다. 사용자가 취소한 경우도 예외를 담은 실패 [Result] 다. */
@Composable
fun rememberOAuthLauncher(onResult: (Result<OAuthAuthorization>) -> Unit): OAuthLauncher {
    val launcher = rememberLauncherForActivityResult(OAuthContract()) { onResult(it) }
    return object : OAuthLauncher {
        override fun launch(provider: OAuthProvider) = launcher.launch(provider)
    }
}
