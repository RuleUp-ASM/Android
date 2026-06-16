package com.ruleup.onboarding.presentation.oauth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import com.ruleup.onboarding.domain.entity.OAuthAuthorization
import com.ruleup.onboarding.domain.entity.OAuthProvider

@Composable
actual fun rememberOAuthLauncher(
    onResult: (Result<OAuthAuthorization>) -> Unit,
): OAuthLauncher {
    val launcher = rememberLauncherForActivityResult(OAuthContract()) { onResult(it) }
    return object : OAuthLauncher {
        override fun launch(provider: OAuthProvider) = launcher.launch(provider)
    }
}
