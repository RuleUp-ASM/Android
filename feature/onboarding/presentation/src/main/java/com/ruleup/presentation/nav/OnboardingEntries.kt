package com.ruleup.presentation.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.ruleup.core.navigation.LoginKey
import com.ruleup.core.navigation.OnboardingIntroKey
import com.ruleup.core.navigation.SignupKey
import com.ruleup.presentation.auth.login.screen.LoginScreen
import com.ruleup.presentation.intro.OnboardingIntroScreen
import com.ruleup.presentation.intro.onboardingPages
import com.ruleup.presentation.profile.ProfileSetupScreen

/**
 * @param onFinishIntro 인트로 완료 → 로그인 (백스택 교체)
 * @param onNavigateHome 로그인 완료 후 홈으로 (cross-feature — 콜백)
 * @param onNavigateSignup 신규 사용자 가입(프로필 설정) 흐름으로 (intra-feature)
 */
fun EntryProviderScope<NavKey>.onboardingEntries(
    onFinishIntro: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateSignup: (signupToken: String) -> Unit,
) {
    entry<OnboardingIntroKey> { OnboardingIntroPager(onFinish = onFinishIntro) }
    entry<LoginKey> {
        LoginScreen(
            onNavigateHome = onNavigateHome,
            onNavigateSignup = onNavigateSignup,
        )
    }
    entry<SignupKey> { key ->
        ProfileSetupScreen(signupToken = key.signupToken, onFinish = onNavigateHome)
    }
}

/**
 * 하나의 nav 목적지 안에서 처리하고, 마지막/건너뛰기에서만 [onFinish] 로 빠져나간다.
 */
@Composable
private fun OnboardingIntroPager(onFinish: () -> Unit) {
    var pageIndex by rememberSaveable { mutableIntStateOf(0) }
    OnboardingIntroScreen(
        page = onboardingPages[pageIndex],
        pageIndex = pageIndex,
        onSkip = onFinish,
        onNext = {
            if (pageIndex == onboardingPages.lastIndex) {
                onFinish()
            } else {
                pageIndex++
            }
        },
    )
}
