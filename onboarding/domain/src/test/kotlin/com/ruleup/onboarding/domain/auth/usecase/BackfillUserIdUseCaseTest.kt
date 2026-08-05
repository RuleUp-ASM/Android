package com.ruleup.onboarding.domain.auth.usecase

import com.ruleup.observability.domain.test.testObservability
import com.ruleup.onboarding.domain.fake.FakeProfileRepository
import com.ruleup.onboarding.domain.fake.FakeTokenRepository
import com.ruleup.onboarding.domain.fake.testUser
import com.ruleup.profile.domain.entity.MyProfile
import com.ruleup.profile.domain.usecase.GetMyProfileUseCase
import kotlinx.coroutines.runBlocking
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BackfillUserIdUseCaseTest {
    private val tokenRepository = FakeTokenRepository()
    private val profileRepository = FakeProfileRepository()

    private val useCase =
        BackfillUserIdUseCase(
            tokenRepository = tokenRepository,
            getMyProfileUseCase = GetMyProfileUseCase(profileRepository),
            observability = testObservability(),
        )

    private fun profile(id: String) =
        MyProfile(
            user = testUser(id = id),
            profileImageStatus = null,
            birthDate = null,
            gender = null,
            agreements = emptyMap(),
        )

    @Test
    fun `userId 가 비어 있으면 프로필 조회로 채운다`() =
        runBlocking {
            profileRepository.myProfile = profile("u-1")

            useCase()

            assertEquals("u-1", tokenRepository.savedUserId)
        }

    @Test
    fun `userId 가 이미 있으면 조회하지 않는다`() =
        runBlocking {
            tokenRepository.savedUserId = "u-existing"
            profileRepository.myProfile = profile("u-other")

            useCase()

            // 매 실행의 비용이 아니라 한 번의 복구여야 한다.
            assertEquals(0, profileRepository.getProfileCallCount)
            assertEquals("u-existing", tokenRepository.savedUserId)
        }

    @Test
    fun `조회가 실패해도 던지지 않는다`() =
        runBlocking {
            profileRepository.profileError = IOException("network down")

            // 던지면 이미 인증 판정을 방출한 부트스트랩 스코프로 무관한 실패가 새어 나간다.
            useCase()

            assertNull(tokenRepository.savedUserId)
        }
}
