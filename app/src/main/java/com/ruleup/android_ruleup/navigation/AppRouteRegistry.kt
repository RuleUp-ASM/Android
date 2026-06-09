package com.ruleup.android_ruleup.navigation

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
        // 스플래시. 앱의 시작 화면 — 자동 로그인 판별 후 홈/인트로로 분기한다(루트).
        AppRoute(
            path = SplashPage.PATH,
            isRoot = true,
            render = { SplashScreen() },
        ),
        // 온보딩 인트로 시작 페이지. 자동 로그인 실패 시 진입하는 로그아웃 상태의 루트.
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
            // 로그아웃 후 재진입 시 홈 등 이전 스택이 남지 않도록 루트로 시작한다.
            isRoot = true,
            render = { LoginScreen(viewModel = metroViewModel<LoginViewModel>()) },
        ),
        // 홈. 온보딩(로그인/가입) 완료 후의 루트 화면 — 뒤로가기로 가입 플로우에 돌아가지 않는다.
        AppRoute(
            path = HomePage.PATH,
            isRoot = true,
            render = { HomeScreen() },
        ),
        // 챌린지 생성 2개 페이지. Activity 스코프의 단일 CreateChallengeViewModel 을 공유한다.
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
        // 프로필 설정(신규 가입) 5개 페이지. 모두 Activity 스코프의 단일 ProfileViewModel 을
        // 공유하므로 입력값이 페이지 이동에도 누적된다. signupToken 은 첫 페이지(아이콘)에 args 로 전달.
        // syntheticStack: deep-link 중간 진입 시 아이콘부터 해당 페이지까지를 백스택으로 복원한다
        // (토큰은 아이콘 키에 실어 공유 ViewModel 이 받도록 한다).
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
