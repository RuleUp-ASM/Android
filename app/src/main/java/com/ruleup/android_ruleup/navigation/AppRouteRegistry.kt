package com.ruleup.android_ruleup.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import com.ruleup.android_ruleup.home.HomeScreen
import com.ruleup.challenge.domain.navigation.ChallengeConfirmPage
import com.ruleup.challenge.domain.navigation.ChallengeCreatePage
import com.ruleup.challenge.domain.navigation.ChallengeDetailPage
import com.ruleup.challenge.domain.navigation.ChallengeExploreListPage
import com.ruleup.challenge.domain.navigation.ChallengeExplorePage
import com.ruleup.challenge.domain.navigation.ChallengeNoticeDetailPage
import com.ruleup.challenge.domain.navigation.ChallengeNoticeEditPage
import com.ruleup.challenge.domain.navigation.ChallengeNoticesPage
import com.ruleup.challenge.domain.navigation.ChallengeRankingPage
import com.ruleup.challenge.domain.navigation.ChallengeTargetsPage
import com.ruleup.challenge.domain.navigation.WatcherInvitationPage
import com.ruleup.challenge.presentation.create.ChallengeConfirmScreen
import com.ruleup.challenge.presentation.create.ChallengeCreateScreen
import com.ruleup.challenge.presentation.detail.ChallengeDetailScreen
import com.ruleup.challenge.presentation.explore.ExploreScreen
import com.ruleup.challenge.presentation.explore.list.ExploreListScreen
import com.ruleup.challenge.presentation.notice.NoticeDetailScreen
import com.ruleup.challenge.presentation.notice.NoticeEditScreen
import com.ruleup.challenge.presentation.notice.NoticeListScreen
import com.ruleup.challenge.presentation.ranking.RankingScreen
import com.ruleup.challenge.presentation.targets.ChallengeTargetsScreen
import com.ruleup.challenge.presentation.watcher.invitation.WatcherInvitationScreen
import com.ruleup.onboarding.domain.navigation.HomePage
import com.ruleup.onboarding.domain.navigation.IntroPromisePage
import com.ruleup.onboarding.domain.navigation.IntroTrustPage
import com.ruleup.onboarding.domain.navigation.IntroVerifyPage
import com.ruleup.onboarding.domain.navigation.LoginPage
import com.ruleup.onboarding.domain.navigation.ProfileAgreementPage
import com.ruleup.onboarding.domain.navigation.ProfileIconPage
import com.ruleup.onboarding.domain.navigation.ProfileInterestPage
import com.ruleup.onboarding.domain.navigation.ProfileNicknamePage
import com.ruleup.onboarding.domain.navigation.ProfilePermissionPage
import com.ruleup.onboarding.domain.navigation.SplashPage
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
import com.ruleup.profile.domain.navigation.MyHomePage
import com.ruleup.profile.domain.navigation.MyTemperaturePage
import com.ruleup.profile.domain.navigation.ReputationHistoryPage
import com.ruleup.profile.presentation.history.ReputationHistoryScreen
import com.ruleup.profile.presentation.home.MyHomeScreen
import com.ruleup.profile.presentation.temperature.MyTemperatureScreen
import com.ruleup.verification.domain.navigation.VerificationDetailPage
import com.ruleup.verification.domain.navigation.VerificationLocationPage
import com.ruleup.verification.domain.navigation.VerificationManualPage
import com.ruleup.verification.domain.navigation.VerificationProgressPage
import com.ruleup.verification.presentation.detail.VerificationDetailScreen
import com.ruleup.verification.presentation.location.VerificationLocationScreen
import com.ruleup.verification.presentation.manual.VerificationManualScreen
import com.ruleup.verification.presentation.progress.VerificationProgressScreen

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
            render = { LoginScreen(viewModel = hiltViewModel<LoginViewModel>()) },
        ),
        AppRoute(
            path = HomePage.PATH,
            isRoot = true,
            isBottomTab = true,
            render = { HomeScreen() },
        ),
        AppRoute(
            path = ChallengeExplorePage.PATH,
            isBottomTab = true,
            // 탐색은 홈 위에 쌓인 탭 화면: 뒤로가기 시 홈으로 돌아간다.
            syntheticStack = {
                listOf(
                    GenericNavKey(HomePage.PATH),
                    GenericNavKey(ChallengeExplorePage.PATH),
                )
            },
            render = { ExploreScreen() },
        ),
        AppRoute(
            path = MyHomePage.PATH,
            isBottomTab = true,
            // 마이는 홈 위에 쌓인 탭 화면: 뒤로가기 시 홈으로 돌아간다(탐색 탭과 동일 규칙).
            syntheticStack = {
                listOf(
                    GenericNavKey(HomePage.PATH),
                    GenericNavKey(MyHomePage.PATH),
                )
            },
            render = { MyHomeScreen() },
        ),
        AppRoute(
            path = MyTemperaturePage.PATH,
            render = { MyTemperatureScreen() },
        ),
        AppRoute(
            path = ReputationHistoryPage.PATH,
            render = { ReputationHistoryScreen() },
        ),
        AppRoute(
            path = ChallengeExploreListPage.PATH,
            render = { args ->
                ExploreListScreen(
                    category = args[ChallengeExploreListPage.ARG_CATEGORY],
                    sort = args[ChallengeExploreListPage.ARG_SORT],
                )
            },
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
            path = ChallengeDetailPage.PATH,
            render = { args ->
                ChallengeDetailScreen(challengeId = args[ChallengeDetailPage.ARG_CHALLENGE_ID].orEmpty())
            },
        ),
        AppRoute(
            path = ChallengeTargetsPage.PATH,
            render = { args ->
                ChallengeTargetsScreen(challengeId = args[ChallengeTargetsPage.ARG_CHALLENGE_ID].orEmpty())
            },
        ),
        AppRoute(
            path = ChallengeNoticesPage.PATH,
            render = { args ->
                NoticeListScreen(
                    challengeId = args[ChallengeNoticesPage.ARG_CHALLENGE_ID].orEmpty(),
                    canManage = args[ChallengeNoticesPage.ARG_CAN_MANAGE].toBoolean(),
                )
            },
        ),
        AppRoute(
            path = ChallengeNoticeDetailPage.PATH,
            render = { args ->
                NoticeDetailScreen(
                    challengeId = args[ChallengeNoticeDetailPage.ARG_CHALLENGE_ID].orEmpty(),
                    noticeId = args[ChallengeNoticeDetailPage.ARG_NOTICE_ID].orEmpty(),
                    canManage = args[ChallengeNoticeDetailPage.ARG_CAN_MANAGE].toBoolean(),
                )
            },
        ),
        AppRoute(
            path = ChallengeNoticeEditPage.PATH,
            render = { args ->
                NoticeEditScreen(
                    challengeId = args[ChallengeNoticeEditPage.ARG_CHALLENGE_ID].orEmpty(),
                    noticeId = args[ChallengeNoticeEditPage.ARG_NOTICE_ID],
                )
            },
        ),
        AppRoute(
            path = ChallengeRankingPage.PATH,
            render = { args ->
                RankingScreen(challengeId = args[ChallengeRankingPage.ARG_CHALLENGE_ID].orEmpty())
            },
        ),
        AppRoute(
            path = WatcherInvitationPage.PATH,
            // 카카오톡 초대 카드 링크(딥링크)로 진입 — 뒤로가기 시 홈으로.
            syntheticStack = { args ->
                listOf(
                    GenericNavKey(HomePage.PATH),
                    GenericNavKey(WatcherInvitationPage.PATH, args),
                )
            },
            render = { args ->
                WatcherInvitationScreen(token = args[WatcherInvitationPage.ARG_TOKEN].orEmpty())
            },
        ),
        AppRoute(
            path = VerificationProgressPage.PATH,
            render = { VerificationProgressScreen() },
        ),
        AppRoute(
            path = VerificationDetailPage.PATH,
            render = { args ->
                VerificationDetailScreen(challengeId = args[VerificationDetailPage.ARG_CHALLENGE_ID].orEmpty())
            },
        ),
        AppRoute(
            path = VerificationManualPage.PATH,
            render = { args ->
                VerificationManualScreen(challengeId = args[VerificationManualPage.ARG_CHALLENGE_ID].orEmpty())
            },
        ),
        AppRoute(
            path = VerificationLocationPage.PATH,
            render = { args ->
                VerificationLocationScreen(
                    challengeId = args[VerificationLocationPage.ARG_CHALLENGE_ID].orEmpty(),
                    defaultRadiusM = args[VerificationLocationPage.ARG_RADIUS]?.toFloatOrNull() ?: 500f,
                    dwellMinutes = args[VerificationLocationPage.ARG_DWELL]?.toIntOrNull() ?: 60,
                    targetPackages =
                        args[VerificationLocationPage.ARG_TARGET_PACKAGES]
                            ?.split(VerificationLocationPage.TARGET_PACKAGES_DELIMITER)
                            ?.filter { it.isNotBlank() }
                            .orEmpty(),
                )
            },
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
