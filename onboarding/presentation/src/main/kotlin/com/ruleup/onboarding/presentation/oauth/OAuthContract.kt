package com.ruleup.onboarding.presentation.oauth

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.ruleup.onboarding.domain.entity.OAuthAuthorization
import com.ruleup.onboarding.domain.entity.OAuthProvider

class OAuthContract : ActivityResultContract<OAuthProvider, Result<OAuthAuthorization>>() {
    override fun createIntent(
        context: Context,
        input: OAuthProvider,
    ): Intent =
        Intent(context, OAuthActivity::class.java)
            .putExtra(OAuthActivity.EXTRA_PROVIDER, input.name)

    override fun parseResult(
        resultCode: Int,
        intent: Intent?,
    ): Result<OAuthAuthorization> {
        val providerName = intent?.getStringExtra(OAuthActivity.EXTRA_PROVIDER)
        if (resultCode != Activity.RESULT_OK || intent == null) {
            return Result.failure(
                IllegalStateException(intent?.getStringExtra(OAuthActivity.EXTRA_ERROR) ?: "인증 취소"),
            )
        }
        return runCatching {
            OAuthAuthorization(
                provider = OAuthProvider.valueOf(providerName ?: error("provider 누락")),
                code = intent.getStringExtra(OAuthActivity.EXTRA_CODE)!!,
                codeVerifier = intent.getStringExtra(OAuthActivity.EXTRA_VERIFIER)!!,
                redirectUri = intent.getStringExtra(OAuthActivity.EXTRA_REDIRECT)!!,
            )
        }
    }
}
