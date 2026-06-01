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
import com.ruleup.presentation.login.screen.LoginScreen
import com.ruleup.presentation.onboarding.OnboardingIntroScreen
import com.ruleup.presentation.onboarding.onboardingPages
import com.ruleup.presentation.profile.ProfileSetupScreen

/**
 * 온보딩 feature 가 소유하는 NavEntry 들. `:navigation` 의 [androidx.navigation3.runtime.entryProvider]
 * 안에서 호출해 합쳐진다. 화면 배선은 이 feature 안에 캡슐화되고, 외부(타 feature)로의 이동만 콜백으로 위임한다.
 *
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
    // 프로필 설정 4단계는 하나의 목적지에서 step 상태로 전환된다(ViewModel 공유). 완료 시 홈으로.
    entry<SignupKey> { key ->
        ProfileSetupScreen(signupToken = key.signupToken, onFinish = onNavigateHome)
    }
}

/**
 * 3페이지 인트로 캐러셀. 페이지 넘김은 네비게이션이 아니라 화면 내부 상태이므로
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
