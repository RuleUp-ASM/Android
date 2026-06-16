package com.ruleup.shared.navigation

import com.ruleup.challenge.domain.ChallengeConfirmPage
import com.ruleup.challenge.domain.ChallengeCreatePage
import com.ruleup.challenge.presentation.create.ChallengeConfirmScreen
import com.ruleup.challenge.presentation.create.ChallengeCreateScreen
import com.ruleup.onboarding.domain.HomePage
import com.ruleup.onboarding.domain.IntroPromisePage
import com.ruleup.onboarding.domain.IntroTrustPage
import com.ruleup.onboarding.domain.IntroVerifyPage
import com.ruleup.onboarding.domain.LoginPage
import com.ruleup.onboarding.domain.ProfileAgreementPage
import com.ruleup.onboarding.domain.ProfileIconPage
import com.ruleup.onboarding.domain.ProfileInterestPage
import com.ruleup.onboarding.domain.ProfileNicknamePage
import com.ruleup.onboarding.domain.ProfilePermissionPage
import com.ruleup.onboarding.domain.SplashPage
import com.ruleup.onboarding.presentation.home.HomeScreen
import com.ruleup.onboarding.presentation.intro.component.IntroContent
import com.ruleup.onboarding.presentation.intro.screen.LoginScreen
import com.ruleup.onboarding.presentation.intro.screen.onboardingPages
import com.ruleup.onboarding.presentation.intro.viewmodel.LoginViewModel
import com.ruleup.onboarding.presentation.profile.ProfileAgreementScreen
import com.ruleup.onboarding.presentation.profile.ProfileIconScreen
import com.ruleup.onboarding.presentation.profile.ProfileInterestScreen
import com.ruleup.onboarding.presentation.profile.ProfileNicknameScreen
import com.ruleup.onboarding.presentation.profile.ProfilePermissionScreen
import com.ruleup.onboarding.presentation.splash.SplashScreen
import dev.zacsweers.metrox.viewmodel.metroViewModel

/**
 * 앱의 모든 페이지 메타데이터 + 렌더러 모음.
 * 새 화면 추가 시 본 리스트에 한 줄을 더한다.
 */
val appRoutes: List<AppRoute> =
    listOf(
        AppRoute(
            path = SplashPage.PATH,
            isRoot = true,
            render = { SplashScreen() },
        ),
        AppRoute(
            path = IntroPromisePage.PATH,
            isRoot = true,
            render = { IntroContent(page = onboardingPages[0], pageIndex = 0) },
        ),
        AppRoute(
            path = IntroVerifyPage.PATH,
            syntheticStack = {
                listOf(
                    GenericNavKey(IntroPromisePage.PATH),
                    GenericNavKey(IntroVerifyPage.PATH),
                )
            },
            render = { IntroContent(page = onboardingPages[1], pageIndex = 1) },
        ),
        AppRoute(
            path = IntroTrustPage.PATH,
            syntheticStack = {
                listOf(
                    GenericNavKey(IntroPromisePage.PATH),
                    GenericNavKey(IntroVerifyPage.PATH),
                    GenericNavKey(IntroTrustPage.PATH),
                )
            },
            render = { IntroContent(page = onboardingPages[2], pageIndex = 2) },
        ),
        AppRoute(
            path = LoginPage.PATH,
            isRoot = true,
            render = { LoginScreen(viewModel = metroViewModel<LoginViewModel>()) },
        ),
        AppRoute(
            path = HomePage.PATH,
            isRoot = true,
            render = { HomeScreen() },
        ),
        AppRoute(
            path = ChallengeCreatePage.PATH,
            render = { ChallengeCreateScreen() },
        ),
        AppRoute(
            path = ChallengeConfirmPage.PATH,
            syntheticStack = {
                listOf(
                    GenericNavKey(ChallengeCreatePage.PATH),
                    GenericNavKey(ChallengeConfirmPage.PATH),
                )
            },
            render = { ChallengeConfirmScreen() },
        ),
        AppRoute(
            path = ProfileIconPage.PATH,
            render = { args -> ProfileIconScreen(signupToken = args[ProfileIconPage.ARG_SIGNUP_TOKEN].orEmpty()) },
        ),
        AppRoute(
            path = ProfileNicknamePage.PATH,
            syntheticStack = { args ->
                listOf(
                    GenericNavKey(ProfileIconPage.PATH, args),
                    GenericNavKey(ProfileNicknamePage.PATH),
                )
            },
            render = { ProfileNicknameScreen() },
        ),
        AppRoute(
            path = ProfileInterestPage.PATH,
            syntheticStack = { args ->
                listOf(
                    GenericNavKey(ProfileIconPage.PATH, args),
                    GenericNavKey(ProfileNicknamePage.PATH),
                    GenericNavKey(ProfileInterestPage.PATH),
                )
            },
            render = { ProfileInterestScreen() },
        ),
        AppRoute(
            path = ProfilePermissionPage.PATH,
            syntheticStack = { args ->
                listOf(
                    GenericNavKey(ProfileIconPage.PATH, args),
                    GenericNavKey(ProfileNicknamePage.PATH),
                    GenericNavKey(ProfileInterestPage.PATH),
                    GenericNavKey(ProfilePermissionPage.PATH),
                )
            },
            render = { ProfilePermissionScreen() },
        ),
        AppRoute(
            path = ProfileAgreementPage.PATH,
            syntheticStack = { args ->
                listOf(
                    GenericNavKey(ProfileIconPage.PATH, args),
                    GenericNavKey(ProfileNicknamePage.PATH),
                    GenericNavKey(ProfileInterestPage.PATH),
                    GenericNavKey(ProfilePermissionPage.PATH),
                    GenericNavKey(ProfileAgreementPage.PATH),
                )
            },
            render = { ProfileAgreementScreen() },
        ),
    )

val appRouteByPath: Map<String, AppRoute> = appRoutes.associateBy { it.path }

val bottomTabRoutes: List<AppRoute> = appRoutes.filter { it.isBottomTab }
