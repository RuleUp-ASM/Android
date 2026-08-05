package com.ruleup.onboarding.domain.auth.usecase

import com.ruleup.domain.entity.user.AgreementConsents
import com.ruleup.domain.entity.user.AgreementType
import com.ruleup.domain.entity.user.Gender
import com.ruleup.domain.entity.user.TermsVersions
import com.ruleup.domain.entity.user.Token
import com.ruleup.observability.domain.test.testObservability
import com.ruleup.onboarding.domain.auth.entity.AuthSession
import com.ruleup.onboarding.domain.auth.entity.SignupForm
import com.ruleup.onboarding.domain.fake.FakeAuthRepository
import com.ruleup.onboarding.domain.fake.FakeDeviceIdentityRepository
import com.ruleup.onboarding.domain.fake.FakeProfileRepository
import com.ruleup.onboarding.domain.fake.FakeTokenRepository
import com.ruleup.onboarding.domain.fake.testUser
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SignupUseCaseTest {
    private val token = Token("a", "r", "Bearer", 3600)

    @Test
    fun `로컬 이미지가 없으면 업로드 없이 가입하고 토큰을 저장한다`() =
        runBlocking {
            val session = AuthSession(token, testUser())
            val auth = FakeAuthRepository().apply { signupResult = session }
            val profile = FakeProfileRepository()
            val tokens = FakeTokenRepository()

            val result = useCase(auth, profile, tokens)(form(localImageUri = null))

            assertEquals(session.user, result)
            assertEquals(token, tokens.savedToken)
            assertNull(profile.uploadCalledWith)
        }

    @Test
    fun `가입 요청에 기기 식별자를 함께 보낸다`() =
        runBlocking {
            val auth = FakeAuthRepository().apply { signupResult = AuthSession(token, testUser()) }

            useCase(auth, FakeProfileRepository(), FakeTokenRepository())(form(localImageUri = null))

            assertEquals("device-1", auth.signedUpWithDevice?.deviceId)
            assertEquals("install-1", auth.signedUpWithDevice?.installationId)
        }

    @Test
    fun `로컬 이미지가 있으면 업로드한 URL 로 프로필 이미지를 교체한다`() =
        runBlocking {
            val session = AuthSession(token, testUser())
            val auth = FakeAuthRepository().apply { signupResult = session }
            val profile = FakeProfileRepository().apply { uploadResult = "https://cdn/p.png" }
            val tokens = FakeTokenRepository()

            val result = useCase(auth, profile, tokens)(form(localImageUri = "file://p"))

            assertEquals("file://p", profile.uploadCalledWith)
            assertEquals("https://cdn/p.png", result.profileImageUrl)
            // 업로드 API 가 accessToken 을 요구하므로 저장이 먼저 끝나 있어야 한다.
            assertEquals(token, tokens.savedToken)
        }

    @Test
    fun `사진 업로드가 실패해도 가입은 성공으로 끝난다`() =
        runBlocking {
            // 가입은 이미 끝난 상태라 여기서 던지면 계정이 만들어졌는데도 화면은 실패로 보인다.
            val session = AuthSession(token, testUser())
            val auth = FakeAuthRepository().apply { signupResult = session }
            val profile = FakeProfileRepository().apply { uploadError = RuntimeException("413") }
            val tokens = FakeTokenRepository()

            val result = useCase(auth, profile, tokens)(form(localImageUri = "file://p"))

            assertEquals(session.user, result)
            assertEquals(token, tokens.savedToken)
        }

    @Test
    fun `공백 URI 는 업로드하지 않고 원본 사용자 정보를 유지한다`() =
        runBlocking {
            val session = AuthSession(token, testUser())
            val auth = FakeAuthRepository().apply { signupResult = session }
            val profile = FakeProfileRepository()

            val result = useCase(auth, profile, FakeTokenRepository())(form(localImageUri = "   "))

            assertNull(profile.uploadCalledWith)
            assertEquals(session.user, result)
        }

    @Test
    fun `약관은 미체크 항목까지 6종 전부 실어 보낸다`() =
        runBlocking {
            val auth = FakeAuthRepository().apply { signupResult = AuthSession(token, testUser()) }

            useCase(auth, FakeProfileRepository(), FakeTokenRepository())(form(localImageUri = null))

            val sent =
                auth.signedUpForm
                    ?.agreements
                    ?.consents
                    .orEmpty()
            assertEquals(AgreementType.entries.toSet(), sent.keys)
            assertEquals(false, sent[AgreementType.MARKETING]?.agreed)
        }

    private fun useCase(
        auth: FakeAuthRepository,
        profile: FakeProfileRepository,
        tokens: FakeTokenRepository,
    ) = SignupUseCase(auth, FakeDeviceIdentityRepository(), profile, tokens, testObservability())

    private fun form(localImageUri: String?) =
        SignupForm(
            signupToken = "signup-token",
            nickname = "nick",
            interestCategories = emptyList(),
            birthDate = LocalDate.of(2000, 5, 27),
            gender = Gender.NON_BINARY,
            agreements = AgreementConsents.of(AgreementType.REQUIRED.toSet(), TermsVersions(emptyMap())),
            localImageUri = localImageUri,
        )
}
