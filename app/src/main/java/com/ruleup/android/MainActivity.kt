package com.ruleup.android

import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kakao.sdk.auth.AuthCodeClient
import com.kakao.sdk.user.UserApiClient
import com.ruleup.core.designsystem.theme.RuleUpTheme
import dagger.hilt.android.AndroidEntryPoint
import java.security.SecureRandom

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RuleUpTheme {
            }
        }
    }

    /** 기존 카카오 PKCE 로그인 로직. 로그인 화면의 카카오 버튼에서 호출된다. */
    private fun startKakaoLogin() {
        val verifier = PkceUtil.generateCodeVerifier()
        val callback: (String?, Throwable?) -> Unit = { code, error ->
            when {
                error != null -> Log.e("KakaoLogin", "실패", error)
                code != null -> Log.i("KakaoLogin", "code=$code / verifier=$verifier")
            }
        }
        if (UserApiClient.instance.isKakaoTalkLoginAvailable(this)) {
            AuthCodeClient.instance.authorizeWithKakaoTalk(
                context = this, codeVerifier = verifier, callback = callback,
            )
        } else {
            AuthCodeClient.instance.authorizeWithKakaoAccount(
                context = this, codeVerifier = verifier, callback = callback,
            )
        }
    }
}


object PkceUtil {
    fun generateCodeVerifier(): String {
        val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        return Base64.encodeToString(
            bytes,
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP,
        ) // 43자 base64url 문자열
    }
}
