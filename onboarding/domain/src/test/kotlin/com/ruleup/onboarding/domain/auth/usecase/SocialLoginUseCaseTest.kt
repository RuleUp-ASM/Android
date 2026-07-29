package com.ruleup.onboarding.domain.auth.usecase

import com.ruleup.domain.entity.user.Token
import com.ruleup.onboarding.domain.entity.AuthSession
import com.ruleup.onboarding.domain.entity.LoginResult
import com.ruleup.onboarding.domain.entity.OAuthAuthorization
import com.ruleup.onboarding.domain.entity.OAuthProfile
import com.ruleup.onboarding.domain.entity.OAuthProvider
import com.ruleup.onboarding.domain.entity.OAuthResult
import com.ruleup.onboarding.domain.entity.User
import com.ruleup.onboarding.domain.fake.FakeAuthRepository
import com.ruleup.onboarding.domain.fake.FakeTokenRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SocialLoginUseCaseTest {
    private val authorization =
        OAuthAuthorization(
            provider = OAuthProvider.KAKAO,
            code = "code",
            codeVerifier = "verifier",
            redirectUri = "app://oauth",
        )

    @Test
    fun `기존 회원이면 토큰을 저장하고 로그인 이벤트를 남긴 뒤 GoMain 을 반환한다`() =
        runBlocking {
            val token = Token("a", "r", "Bearer", 3600)
            val session = AuthSession(token, user())
            val auth = FakeAuthRepository().apply { exchangeResult = OAuthResult.ExistingUser(session) }
            val tokens = FakeTokenRepository()

            val result = SocialLoginUseCase(auth, tokens)(authorization)

            assertEquals(LoginResult.GoMain, result)
            assertEquals(token, tokens.savedToken)
        }

    @Test
    fun `신규 회원이면 토큰을 저장하거나 로깅하지 않고 GoSignup 을 반환한다`() =
        runBlocking {
            val auth =
                FakeAuthRepository().apply {
                    exchangeResult =
                        OAuthResult.NewUser(
                            signupToken = "signup-token",
                            signupTokenExpireInSeconds = 600,
                            oauthProfile = OAuthProfile(email = null, profileImageUrlHint = null),
                        )
                }
            val tokens = FakeTokenRepository()

            val result = SocialLoginUseCase(auth, tokens)(authorization)

            assertEquals(LoginResult.GoSignup("signup-token"), result)
            assertNull(tokens.savedToken)
        }

    private fun user() =
        User(
            id = "u1",
            nickname = "nick",
            email = null,
            profileImageUrl = null,
            mannerTemperature = 36.5,
            interestCategories = emptyList(),
        )
}
