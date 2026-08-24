package com.ruleup.android_ruleup.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import com.ruleup.challenge.domain.navigation.ChallengeConfirmPage
import com.ruleup.challenge.domain.navigation.ChallengeCreatePage
import com.ruleup.challenge.domain.navigation.ChallengeDetailPage
import com.ruleup.challenge.domain.navigation.ChallengeExploreListPage
import com.ruleup.challenge.domain.navigation.ChallengeExplorePage
import com.ruleup.challenge.domain.navigation.ChallengeRankingPage
import com.ruleup.challenge.domain.navigation.ChallengeSettingsPage
import com.ruleup.challenge.domain.navigation.ChallengeTargetsPage
import com.ruleup.challenge.presentation.create.ChallengeConfirmScreen
import com.ruleup.challenge.presentation.create.ChallengeCreateScreen
import com.ruleup.challenge.presentation.detail.ChallengeDetailScreen
import com.ruleup.challenge.presentation.explore.ExploreScreen
import com.ruleup.challenge.presentation.explore.list.ExploreListScreen
import com.ruleup.challenge.presentation.ranking.RankingScreen
import com.ruleup.challenge.presentation.settings.ChallengeSettingsScreen
import com.ruleup.challenge.presentation.targets.ChallengeTargetsScreen
import com.ruleup.domain.navigation.RouteAccessPolicy
import com.ruleup.home.presentation.HomeScreen
import com.ruleup.onboarding.domain.navigation.HomePage
import com.ruleup.onboarding.domain.navigation.LoginPage
import com.ruleup.onboarding.domain.navigation.OnboardingBirthPage
import com.ruleup.onboarding.domain.navigation.OnboardingGenderPage
import com.ruleup.onboarding.domain.navigation.OnboardingInterestPage
import com.ruleup.onboarding.domain.navigation.OnboardingNicknamePage
import com.ruleup.onboarding.domain.navigation.OnboardingPhotoPage
import com.ruleup.onboarding.domain.navigation.OnboardingTermsPage
import com.ruleup.onboarding.domain.navigation.SplashPage
import com.ruleup.onboarding.presentation.intro.screen.LoginScreen
import com.ruleup.onboarding.presentation.intro.viewmodel.LoginViewModel
import com.ruleup.onboarding.presentation.onboarding.OnboardingBirthScreen
import com.ruleup.onboarding.presentation.onboarding.OnboardingGenderScreen
import com.ruleup.onboarding.presentation.onboarding.OnboardingInterestScreen
import com.ruleup.onboarding.presentation.onboarding.OnboardingNicknameScreen
import com.ruleup.onboarding.presentation.onboarding.OnboardingPhotoScreen
import com.ruleup.onboarding.presentation.onboarding.OnboardingTermsScreen
import com.ruleup.onboarding.presentation.splash.SplashScreen
import com.ruleup.profile.domain.navigation.FriendInvitePage
import com.ruleup.profile.domain.navigation.MyAppealsPage
import com.ruleup.profile.domain.navigation.MyCalendarPage
import com.ruleup.profile.domain.navigation.MyHomePage
import com.ruleup.profile.domain.navigation.MyStatsPage
import com.ruleup.profile.domain.navigation.MyTemperaturePage
import com.ruleup.profile.domain.navigation.ProfileEditPage
import com.ruleup.profile.domain.navigation.ReputationHistoryPage
import com.ruleup.profile.presentation.appeals.MyAppealsScreen
import com.ruleup.profile.presentation.calendar.MyCalendarScreen
import com.ruleup.profile.presentation.edit.ProfileEditScreen
import com.ruleup.profile.presentation.history.ReputationHistoryScreen
import com.ruleup.profile.presentation.home.MyHomeScreen
import com.ruleup.profile.presentation.invite.FriendInviteScreen
import com.ruleup.profile.presentation.stats.MyStatsScreen
import com.ruleup.profile.presentation.temperature.MyTemperatureScreen
import com.ruleup.verification.domain.entity.SetupAnchors
import com.ruleup.verification.domain.navigation.VerificationLocationPage
import com.ruleup.verification.presentation.location.VerificationLocationScreen
import javax.inject.Inject
import javax.inject.Singleton

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
            path = MyAppealsPage.PATH,
            render = { MyAppealsScreen() },
        ),
        AppRoute(
            path = MyCalendarPage.PATH,
            render = { MyCalendarScreen() },
        ),
        AppRoute(
            path = MyStatsPage.PATH,
            render = { MyStatsScreen() },
        ),
        AppRoute(
            path = ProfileEditPage.PATH,
            render = { ProfileEditScreen() },
        ),
        AppRoute(
            path = FriendInvitePage.PATH,
            render = { FriendInviteScreen() },
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
            path = ChallengeRankingPage.PATH,
            render = { args ->
                RankingScreen(challengeId = args[ChallengeRankingPage.ARG_CHALLENGE_ID].orEmpty())
            },
        ),
        AppRoute(
            path = ChallengeSettingsPage.PATH,
            render = { args ->
                ChallengeSettingsScreen(challengeId = args[ChallengeSettingsPage.ARG_CHALLENGE_ID].orEmpty())
            },
        ),
        AppRoute(
            path = VerificationLocationPage.PATH,
            render = { args ->
                VerificationLocationScreen(
                    challengeId = args[VerificationLocationPage.ARG_CHALLENGE_ID].orEmpty(),
                    // 지도 원을 그릴 반경. 서버 설정값을 셋업 응답으로 받기 전까지 쓰는 표시용 기본값이다.
                    defaultRadiusM =
                        args[VerificationLocationPage.ARG_RADIUS]
                            ?.toFloatOrNull()
                            ?: SetupAnchors.DEFAULT_RADIUS_M,
                    dwellMinutes = args[VerificationLocationPage.ARG_DWELL]?.toIntOrNull() ?: 60,
                    targetPackages =
                        args[VerificationLocationPage.ARG_TARGET_PACKAGES]
                            ?.split(VerificationLocationPage.TARGET_PACKAGES_DELIMITER)
                            ?.filter { it.isNotBlank() }
                            .orEmpty(),
                )
            },
        ),
        // 온보딩 6단계. syntheticStack 은 딥링크·프로세스 복구로 중간 단계에 바로 들어왔을 때
        // 뒤로가기가 앞 단계를 거치도록 스택을 세워 준다.
        AppRoute(
            path = OnboardingNicknamePage.PATH,
            render = { OnboardingNicknameScreen() },
        ),
        AppRoute(
            path = OnboardingInterestPage.PATH,
            syntheticStack = { onboardingStack(OnboardingInterestPage.PATH) },
            render = { OnboardingInterestScreen() },
        ),
        AppRoute(
            path = OnboardingBirthPage.PATH,
            syntheticStack = { onboardingStack(OnboardingInterestPage.PATH, OnboardingBirthPage.PATH) },
            render = { OnboardingBirthScreen() },
        ),
        AppRoute(
            path = OnboardingGenderPage.PATH,
            syntheticStack = {
                onboardingStack(OnboardingInterestPage.PATH, OnboardingBirthPage.PATH, OnboardingGenderPage.PATH)
            },
            render = { OnboardingGenderScreen() },
        ),
        AppRoute(
            path = OnboardingPhotoPage.PATH,
            syntheticStack = {
                onboardingStack(
                    OnboardingInterestPage.PATH,
                    OnboardingBirthPage.PATH,
                    OnboardingGenderPage.PATH,
                    OnboardingPhotoPage.PATH,
                )
            },
            render = { OnboardingPhotoScreen() },
        ),
        AppRoute(
            path = OnboardingTermsPage.PATH,
            syntheticStack = {
                onboardingStack(
                    OnboardingInterestPage.PATH,
                    OnboardingBirthPage.PATH,
                    OnboardingGenderPage.PATH,
                    OnboardingPhotoPage.PATH,
                    OnboardingTermsPage.PATH,
                )
            },
            render = { OnboardingTermsScreen() },
        ),
    )

/** 1단계(닉네임)를 뿌리로, 뒤이은 단계를 순서대로 쌓는다. */
private fun onboardingStack(vararg paths: String): List<GenericNavKey> =
    listOf(GenericNavKey(OnboardingNicknamePage.PATH)) + paths.map { GenericNavKey(it) }

val appRouteByPath: Map<String, AppRoute> = appRoutes.associateBy { it.path }

/**
 * [appRoutes] 를 그대로 읽는 [RouteAccessPolicy] 구현.
 *
 * 모르는 경로는 로그인 요구로 떨어뜨린다 — 딥링크는 외부 입력이라, 등록되지 않은 경로를 공개로
 * 보면 오타 하나가 인증 우회 통로가 된다.
 */
@Singleton
class AppRouteAccessPolicy
    @Inject
    constructor() : RouteAccessPolicy {
        override fun requiresLogin(path: String): Boolean = appRouteByPath[path]?.isLoginRequired ?: true
    }

val bottomTabRoutes: List<AppRoute> = appRoutes.filter { it.isBottomTab }
