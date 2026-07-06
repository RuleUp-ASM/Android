package com.ruleup.onboarding.domain.auth.usecase

import com.ruleup.entity.user.Agreement
import com.ruleup.entity.user.AuthSession
import com.ruleup.entity.user.Token
import com.ruleup.entity.user.User
import com.ruleup.onboarding.domain.auth.model.SignupForm
import com.ruleup.onboarding.domain.fake.FakeAuthRepository
import com.ruleup.onboarding.domain.fake.FakeProfileRepository
import com.ruleup.onboarding.domain.fake.FakeTokenRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SignupUseCaseTest {
    private val token = Token("a", "r", "Bearer", 3600)

    @Test
    fun `로컬 이미지가 없으면 업로드 없이 가입하고 토큰을 저장한다`() =
        runBlocking {
            val session = AuthSession(token, user())
            val auth = FakeAuthRepository().apply { signupResult = session }
            val profile = FakeProfileRepository()
            val tokens = FakeTokenRepository()

            val result = SignupUseCase(auth, profile, tokens)(form(localImageUri = null))

            assertEquals(session.user, result)
            assertEquals(token, tokens.savedToken)
            assertNull(profile.uploadCalledWith)
        }

    @Test
    fun `로컬 이미지가 있으면 업로드한 URL 로 프로필 이미지를 교체한다`() =
        runBlocking {
            val session = AuthSession(token, user())
            val auth = FakeAuthRepository().apply { signupResult = session }
            val profile = FakeProfileRepository().apply { uploadResult = "https://cdn/p.png" }
            val tokens = FakeTokenRepository()

            val result = SignupUseCase(auth, profile, tokens)(form(localImageUri = "file://p"))

            assertEquals("file://p", profile.uploadCalledWith)
            assertEquals("https://cdn/p.png", result.profileImageUrl)
            assertEquals(token, tokens.savedToken)
        }

    @Test
    fun `공백 URI 는 업로드하지 않고 원본 사용자 정보를 유지한다`() =
        runBlocking {
            val session = AuthSession(token, user())
            val auth = FakeAuthRepository().apply { signupResult = session }
            val profile = FakeProfileRepository()
            val tokens = FakeTokenRepository()

            val result = SignupUseCase(auth, profile, tokens)(form(localImageUri = "   "))

            assertNull(profile.uploadCalledWith)
            assertEquals(session.user, result)
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

    private fun form(localImageUri: String?) =
        SignupForm(
            signupToken = "signup-token",
            nickname = "nick",
            interestCategories = emptyList(),
            agreements = Agreement(terms = true, privacy = true, marketing = false),
            localImageUri = localImageUri,
        )
}
