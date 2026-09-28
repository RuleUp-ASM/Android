package com.ruleup.onboarding.presentation.oauth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import com.ruleup.onboarding.domain.auth.entity.OAuthAuthorization
import com.ruleup.onboarding.domain.auth.entity.OAuthProvider

/** 소셜 로그인(OAuth) 플로우를 추상화한 런처. */
interface OAuthLauncher {
    fun launch(provider: OAuthProvider)
}

/** 인가 결과를 [onResult] 로 돌려준다. */
@Composable
fun rememberOAuthLauncher(onResult: (Result<OAuthAuthorization>) -> Unit): OAuthLauncher {
    val launcher = rememberLauncherForActivityResult(OAuthContract()) { onResult(it) }
    return object : OAuthLauncher {
        override fun launch(provider: OAuthProvider) = launcher.launch(provider)
    }
}
