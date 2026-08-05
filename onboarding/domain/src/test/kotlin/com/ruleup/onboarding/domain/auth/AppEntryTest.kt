package com.ruleup.onboarding.domain.auth

import com.ruleup.domain.entity.user.Token
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.PendingDeepLink
import com.ruleup.domain.navigation.RouteAccessPolicy
import com.ruleup.domain.token.RefreshedSession
import com.ruleup.observability.domain.test.testObservability
import com.ruleup.onboarding.domain.auth.usecase.AutoLoginUseCase
import com.ruleup.onboarding.domain.fake.FakeAuthRepository
import com.ruleup.onboarding.domain.fake.FakeIntroRepository
import com.ruleup.onboarding.domain.fake.FakeTokenRepository
import com.ruleup.onboarding.domain.intro.usecase.LoadIntroUseCase
import com.ruleup.onboarding.domain.navigation.HomePage
import com.ruleup.onboarding.domain.navigation.LoginPage
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 진입 목적지 판정. 세션·보류 딥링크·경로 공개 여부 세 축이 만나는 곳이라, 화면이 아니라 여기서
 * 고정한다.
 */
class AppEntryTest {
    private val deepLink = NavRoute("challenge/detail", mapOf("challengeId" to "ch1"))

    @Test
    fun `딥링크가 없으면 인증 여부로 홈과 로그인을 가른다`() =
        runBlocking {
            assertEquals(AppEntry.Start(HomePage.toRoute()), resolve(authenticated = true))
            assertEquals(AppEntry.Start(LoginPage.toRoute()), resolve(authenticated = false))
        }

    @Test
    fun `인증됐으면 보류 딥링크로 간다`() =
        runBlocking {
            assertEquals(AppEntry.DeepLink(deepLink), resolve(authenticated = true, pending = deepLink))
        }

    @Test
    fun `미인증이면 딥링크를 버리고 로그인으로 보낸다`() =
        runBlocking {
            // 세션 없이 목적지를 띄우면 API 가 401 을 받고 사용자는 목적지가 아니라 로그인을 본다.
            assertEquals(
                AppEntry.Start(LoginPage.toRoute()),
                resolve(authenticated = false, pending = deepLink),
            )
        }

    @Test
    fun `미인증이어도 로그인이 필요 없는 화면이면 그대로 연다`() =
        runBlocking {
            assertEquals(
                AppEntry.DeepLink(deepLink),
                resolve(authenticated = false, pending = deepLink, requiresLogin = false),
            )
        }

    @Test
    fun `세션이 끊기면 판정을 다시 돌린다`() =
        runBlocking {
            // 다른 기기 로그인·refreshToken 만료는 TokenAuthenticator 가 토큰을 지우는 것으로만
            // 드러난다. 그 순간 진입 판정으로 되돌아가야 사용자가 빈 세션으로 화면에 남지 않는다.
            val tokens = FakeTokenRepository(refreshToken = "r1")
            val auth =
                FakeAuthRepository().apply {
                    refreshResult = RefreshedSession(Token("a", "r2", "Bearer", 3600), userId = "u-1")
                }
            val bootstrap = bootstrap(tokens, auth, pending = null, requiresLogin = true)
            bootstrap.start()
            assertEquals(
                AppEntry.Start(HomePage.toRoute()),
                (bootstrap.state.first { it is SessionBootstrapState.Resolved } as SessionBootstrapState.Resolved).entry,
            )

            tokens.clear()

            assertEquals(
                AppEntry.Start(LoginPage.toRoute()),
                bootstrap.state
                    .filterIsInstance<SessionBootstrapState.Resolved>()
                    .first { it.entry == AppEntry.Start(LoginPage.toRoute()) }
                    .entry,
            )
        }

    private fun bootstrap(
        tokens: FakeTokenRepository,
        auth: FakeAuthRepository,
        pending: NavRoute?,
        requiresLogin: Boolean,
    ) = SessionBootstrap(
        loadIntroUseCase = LoadIntroUseCase(FakeIntroRepository().apply { error = RuntimeException("skip") }),
        pendingDeepLink = PendingDeepLink().apply { set(pending) },
        routeAccessPolicy = RouteAccessPolicy { requiresLogin },
        observability = testObservability(),
        tokenRepository = tokens,
        autoLoginUseCase = AutoLoginUseCase(auth, tokens),
    )

    private suspend fun resolve(
        authenticated: Boolean,
        pending: NavRoute? = null,
        requiresLogin: Boolean = true,
    ): AppEntry {
        val tokens = FakeTokenRepository(refreshToken = if (authenticated) "r1" else null)
        val auth =
            FakeAuthRepository().apply {
                refreshResult = RefreshedSession(Token("a", "r2", "Bearer", 3600), userId = "u-1")
            }
        val bootstrap =
            SessionBootstrap(
                loadIntroUseCase = LoadIntroUseCase(FakeIntroRepository().apply { error = RuntimeException("skip") }),
                pendingDeepLink = PendingDeepLink().apply { set(pending) },
                routeAccessPolicy = RouteAccessPolicy { requiresLogin },
                observability = testObservability(),
                tokenRepository = tokens,
                autoLoginUseCase =
                    com.ruleup.onboarding.domain.auth.usecase
                        .AutoLoginUseCase(auth, tokens),
            )
        bootstrap.start()
        return (bootstrap.state.first { it is SessionBootstrapState.Resolved } as SessionBootstrapState.Resolved).entry
    }
}
