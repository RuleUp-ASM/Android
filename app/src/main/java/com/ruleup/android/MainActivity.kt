package com.ruleup.android

import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.kakao.sdk.auth.AuthCodeClient
import com.kakao.sdk.user.UserApiClient
import com.ruleup.core.designsystem.theme.RuleUpTheme
import com.ruleup.presentation.login.LoginScreen
import com.ruleup.presentation.onboarding.OnboardingIntroScreen
import com.ruleup.presentation.onboarding.onboardingPages
import com.ruleup.presentation.profile.InterestScreen
import com.ruleup.presentation.profile.NicknameScreen
import com.ruleup.presentation.profile.PermissionScreen
import com.ruleup.presentation.profile.ProfileIconScreen
import com.ruleup.presentation.splash.SplashScreen
import dagger.hilt.android.AndroidEntryPoint
import java.security.SecureRandom

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RuleUpTheme {
                OnboardingFlow(
                    modifier = Modifier.fillMaxSize(),
                    onKakaoLogin = ::startKakaoLogin,
                )
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

/** 온보딩 단계. */
private enum class OnboardingStep {
    Splash, Intro1, Intro2, Intro3, Login, ProfileIcon, Nickname, Interest, Permission, Done
}

/**
 * 디자인 시스템 화면들을 순서대로 이어 붙인 온보딩 플로우.
 *
 * 화면들은 상태 없는 프레젠테이션이므로, 단계 전환만 여기서 관리한다.
 * 정식 네비게이션(Navigation3) 도입 전까지의 임시 호스트.
 */
@Composable
private fun OnboardingFlow(
    modifier: Modifier = Modifier,
    onKakaoLogin: () -> Unit = {},
) {
    var step by remember { mutableStateOf(OnboardingStep.Splash) }

    when (step) {
        OnboardingStep.Splash -> {
            LaunchedEffect(Unit) {
                delay(1500)
                step = OnboardingStep.Intro1
            }
            SplashScreen(modifier = modifier)
        }
        OnboardingStep.Intro1 -> OnboardingIntroScreen(
            page = onboardingPages[0],
            pageIndex = 0,
            modifier = modifier,
            onSkip = { step = OnboardingStep.Login },
            onNext = { step = OnboardingStep.Intro2 },
        )
        OnboardingStep.Intro2 -> OnboardingIntroScreen(
            page = onboardingPages[1],
            pageIndex = 1,
            modifier = modifier,
            onSkip = { step = OnboardingStep.Login },
            onNext = { step = OnboardingStep.Intro3 },
        )
        OnboardingStep.Intro3 -> OnboardingIntroScreen(
            page = onboardingPages[2],
            pageIndex = 2,
            modifier = modifier,
            onNext = { step = OnboardingStep.Login },
        )
        OnboardingStep.Login -> LoginScreen(
            modifier = modifier,
            onProviderClick = { label ->
                if (label.startsWith("카카오")) onKakaoLogin() else step = OnboardingStep.ProfileIcon
            },
        )
        OnboardingStep.ProfileIcon -> ProfileIconScreen(modifier = modifier)
        OnboardingStep.Nickname -> NicknameScreen(modifier = modifier)
        OnboardingStep.Interest -> InterestScreen(modifier = modifier)
        OnboardingStep.Permission -> PermissionScreen(modifier = modifier)
        OnboardingStep.Done -> Unit
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
