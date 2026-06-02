package com.ruleup.data.auth.oauth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import kotlinx.coroutines.CompletableDeferred
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService

/**
 * [GoogleOAuthAuthorizer] 와 [GoogleAuthActivity] 사이를 잇는 일회용 브릿지.
 * suspend 함수가 인가 응답을 기다리도록 [pending] 에 결과를 전달한다.
 */
internal object GoogleAuthBridge {
    var request: AuthorizationRequest? = null
    var pending: CompletableDeferred<AuthorizationResponse>? = null
}

/**
 * AppAuth 인가 Intent 를 실행하고 그 결과(코드/에러)를 받아 [GoogleAuthBridge] 로 돌려준다.
 * UI 가 없는 통과용 Activity 다. app 매니페스트에 등록되어 있어야 한다.
 */
class GoogleAuthActivity : Activity() {
    private lateinit var service: AuthorizationService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        service = AuthorizationService(this)
        val request = GoogleAuthBridge.request
        if (request == null) {
            finish()
            return
        }
        startActivityForResult(service.getAuthorizationRequestIntent(request), RC_AUTH)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_AUTH) {
            val pending = GoogleAuthBridge.pending
            GoogleAuthBridge.pending = null
            GoogleAuthBridge.request = null

            val response = data?.let { AuthorizationResponse.fromIntent(it) }
            val error = data?.let { AuthorizationException.fromIntent(it) }
            when {
                response != null -> {
                    pending?.complete(response)
                }

                else -> {
                    pending?.completeExceptionally(
                        error ?: IllegalStateException("구글 인증이 취소되었습니다."),
                    )
                }
            }
        }
        service.dispose()
        finish()
    }

    companion object {
        private const val RC_AUTH = 1001
    }
}
