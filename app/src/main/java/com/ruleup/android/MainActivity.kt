package com.ruleup.android

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.kakao.sdk.auth.AuthCodeClient
import com.kakao.sdk.user.UserApiClient
import com.ruleup.android.ui.theme.AndroidTheme
import dagger.hilt.android.AndroidEntryPoint
import java.security.SecureRandom
import android.util.Base64
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndroidTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    KakaoLoginTestButton()
                }
            }
        }
    }
}

@Composable
fun Greeting(
    name: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "Hello $name!",
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun GreetingPreview() {
    AndroidTheme {
        Greeting("Android")
    }
}

@Composable
fun KakaoLoginTestButton() {
    val context = LocalContext.current
    Button(onClick = {
        val verifier = PkceUtil.generateCodeVerifier()
        val callback: (String?, Throwable?) -> Unit = { code, error ->
            when {
                error != null -> Log.e("KakaoLogin", "실패", error)
                code != null  -> Log.i("KakaoLogin", "code=$code / verifier=$verifier")
            }
        }
        if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
            AuthCodeClient.instance.authorizeWithKakaoTalk(
                context = context, codeVerifier = verifier, callback = callback,
            )
        } else {
            AuthCodeClient.instance.authorizeWithKakaoAccount(
                context = context, codeVerifier = verifier, callback = callback,
            )
        }
    }) { Text("카카오 로그인 테스트") }
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
