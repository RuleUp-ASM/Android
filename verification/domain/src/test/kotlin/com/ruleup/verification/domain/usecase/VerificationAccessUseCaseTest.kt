package com.ruleup.verification.domain.usecase

import com.ruleup.domain.entity.user.AgreementType
import com.ruleup.domain.entity.user.TermsVersions
import com.ruleup.onboarding.domain.intro.repository.IntroRepository
import com.ruleup.profile.domain.entity.AgreementState
import com.ruleup.profile.domain.entity.AgreementStatus
import com.ruleup.profile.domain.entity.AgreementSubmission
import com.ruleup.profile.domain.repository.AccountRepository
import com.ruleup.verification.domain.entity.PermissionSnapshot
import com.ruleup.verification.domain.entity.PermissionState
import com.ruleup.verification.domain.repository.PermissionStatusProvider
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** OS 권한을 허용해도 서버 동의는 별도로 남아야 한다. */
class CheckVerificationAccessUseCaseTest {
    @Test
    fun `위치와 건강 권한이 허용돼 있어도 두 동의가 없으면 모두 요구한다`() =
        runBlocking {
            val result = checkAccess(AccessAccountRepository())(listOf("ACCESS_FINE_LOCATION", "READ_STEPS", "READ_SLEEP"))

            assertTrue(result.missingPermissions.isEmpty())
            assertEquals(listOf(AgreementType.LOCATION_INFO, AgreementType.HEALTH_INFO), result.missingConsents)
        }

    @Test
    fun `이미 동의했어도 기기 권한이 거부돼 있으면 권한만 요구한다`() =
        runBlocking {
            val account = AccessAccountRepository(agreed = setOf(AgreementType.HEALTH_INFO))
            val denied = accessSnapshot().copy(healthSteps = PermissionState.DENIED)
            val result = CheckVerificationAccessUseCase(PermissionStatusProvider { denied }, account)(listOf("READ_STEPS"))

            assertEquals(listOf("READ_STEPS"), result.missingPermissions)
            assertTrue(result.missingConsents.isEmpty())
            assertEquals(denied, result.permissions)
        }

    @Test
    fun `필요 권한이 없으면 서버 동의를 조회하지 않는다`() =
        runBlocking {
            val account = AccessAccountRepository()
            val result = checkAccess(account)(emptyList())

            assertTrue(result.missingConsents.isEmpty())
            assertTrue(result.missingPermissions.isEmpty())
            assertEquals(0, account.readCount)
        }

    @Test
    fun `사용 기록과 미지원 권한은 위치나 건강 동의를 요구하지 않는다`() =
        runBlocking {
            val account = AccessAccountRepository()
            val result = checkAccess(account)(listOf("PACKAGE_USAGE_STATS", "FUTURE_PERMISSION"))

            assertTrue(result.missingConsents.isEmpty())
            assertEquals(0, account.readCount)
        }

    @Test
    fun `동의 조회가 실패하면 허용된 것으로 반환하지 않는다`(): Unit =
        runBlocking {
            val account = AccessAccountRepository(readError = IllegalStateException("조회 실패"))

            assertFailsWith<IllegalStateException> { checkAccess(account)(listOf("READ_STEPS")) }
        }

    @Test
    fun `기기 권한 조회가 실패하면 허용된 것으로 반환하지 않는다`(): Unit =
        runBlocking {
            val check = CheckVerificationAccessUseCase(PermissionStatusProvider { error("권한 조회 실패") }, AccessAccountRepository())

            assertFailsWith<IllegalStateException> { check(listOf("READ_STEPS")) }
        }

    private fun checkAccess(account: AccountRepository) =
        CheckVerificationAccessUseCase(PermissionStatusProvider { accessSnapshot() }, account)
}

/** 제출에 실패한 동의가 완료 처리되거나 약관 버전이 바뀌는 회귀를 막는다. */
class AgreeVerificationConsentUseCaseTest {
    @Test
    fun `두 정보 수집에 함께 동의하면 한 번의 요청으로 중복 없이 기록한다`() =
        runBlocking {
            val account = AccessAccountRepository()

            AgreeVerificationConsentUseCase(
                account,
                AccessIntroRepository,
            )(listOf(AgreementType.LOCATION_INFO, AgreementType.HEALTH_INFO, AgreementType.LOCATION_INFO))

            assertEquals(listOf(AgreementType.LOCATION_INFO, AgreementType.HEALTH_INFO), account.submitted.map { it.type })
            assertEquals(1, account.submitCount)
        }

    @Test
    fun `새로 받을 동의가 없으면 약관을 조회하거나 제출하지 않는다`() =
        runBlocking {
            val account = AccessAccountRepository()

            AgreeVerificationConsentUseCase(account, AccessIntroRepository)(emptyList())

            assertEquals(0, account.readCount)
            assertEquals(0, account.submitCount)
        }

    @Test
    fun `받은 적 없는 동의는 인트로의 약관 버전으로 기록한다`() =
        runBlocking {
            val account = AccessAccountRepository()

            AgreeVerificationConsentUseCase(account, AccessIntroRepository)(listOf(AgreementType.LOCATION_INFO))

            assertEquals(listOf(AgreementSubmission(AgreementType.LOCATION_INFO, true, "2.0")), account.submitted)
        }

    @Test
    fun `기존 동의 버전이 있으면 조회한 버전으로 기록한다`() =
        runBlocking {
            val account = AccessAccountRepository(version = "1.5")

            AgreeVerificationConsentUseCase(account, AccessIntroRepository)(listOf(AgreementType.HEALTH_INFO))

            assertEquals("1.5", account.submitted.single().version)
        }

    @Test
    fun `동의 저장이 실패하면 실패를 호출자에게 전달한다`() =
        runBlocking {
            val account = AccessAccountRepository(submitError = IllegalStateException("저장 실패"))

            assertFailsWith<IllegalStateException> {
                AgreeVerificationConsentUseCase(account, AccessIntroRepository)(listOf(AgreementType.LOCATION_INFO))
            }
            assertTrue(account.submitted.isEmpty())
        }

    @Test
    fun `위치와 건강 이외의 약관은 인증 동의로 제출하지 않는다`() =
        runBlocking {
            val account = AccessAccountRepository()
            val other = AgreementType.entries.first { it != AgreementType.LOCATION_INFO && it != AgreementType.HEALTH_INFO }

            assertFailsWith<IllegalArgumentException> { AgreeVerificationConsentUseCase(account, AccessIntroRepository)(listOf(other)) }
            assertEquals(0, account.readCount)
            assertTrue(account.submitted.isEmpty())
        }
}

private fun accessSnapshot() =
    PermissionSnapshot(
        location = PermissionState.GRANTED,
        backgroundLocation = PermissionState.GRANTED,
        usageStats = PermissionState.GRANTED,
        postNotifications = PermissionState.GRANTED,
        healthDistance = PermissionState.GRANTED,
        healthSteps = PermissionState.GRANTED,
        healthSleep = PermissionState.GRANTED,
        healthBackground = PermissionState.GRANTED,
    )

private class AccessAccountRepository(
    private val agreed: Set<AgreementType> = emptySet(),
    private val version: String? = null,
    private val readError: Throwable? = null,
    private val submitError: Throwable? = null,
) : AccountRepository {
    var readCount = 0
    var submitCount = 0
    val submitted = mutableListOf<AgreementSubmission>()

    override suspend fun getAgreements(): AgreementStatus {
        readCount++
        readError?.let { throw it }
        return AgreementStatus(
            agreements =
                AgreementType.entries.map {
                    AgreementState(it, required = false, agreed = it in agreed, version = version, agreedAt = null)
                },
            reconsentRequired = emptyList(),
        )
    }

    override suspend fun submitAgreements(submissions: List<AgreementSubmission>): AgreementStatus {
        submitCount++
        submitError?.let { throw it }
        submitted += submissions
        return getAgreements()
    }

    override suspend fun getSanctions() = error("이 테스트는 제재 이력을 조회하지 않는다")
}

private object AccessIntroRepository : IntroRepository {
    override suspend fun getIntro() = error("인트로는 다시 요청하지 않는다")

    override fun lastTermsVersions() = TermsVersions(AgreementType.entries.associateWith { "2.0" })
}
