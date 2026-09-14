package com.ruleup.challenge.presentation.common

import com.ruleup.challenge.domain.entity.VerificationMethod
import com.ruleup.challenge.presentation.fake.FakeAccountRepository
import com.ruleup.domain.entity.user.AgreementType
import com.ruleup.onboarding.domain.fake.FakeIntroRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** 위치·건강 개별 동의는 가입 때 받지 않는다 — 이 인증을 처음 쓰는 순간을 놓치면 끝내 받지 못한다(ONB-14·ONB-15). */
class SensitiveConsentTest {
    @Test
    fun `위치 인증 방은 위치 개별 동의가 없으면 그 항목을 요구한다`() =
        runBlocking {
            val consent = SensitiveConsent(FakeAccountRepository(), FakeIntroRepository())

            assertEquals(AgreementType.LOCATION_INFO, consent.missingFor(VerificationMethod.GPS_PRESENCE))
        }

    @Test
    fun `건강 인증 방은 이미 동의했으면 다시 묻지 않는다`() =
        runBlocking {
            val consent = SensitiveConsent(FakeAccountRepository(agreed = setOf(AgreementType.HEALTH_INFO)), FakeIntroRepository())

            assertNull(consent.missingFor(VerificationMethod.HEALTH))
        }

    @Test
    fun `기상 인증처럼 위치·건강을 쓰지 않으면 개별 동의가 필요 없다`() =
        runBlocking {
            val consent = SensitiveConsent(FakeAccountRepository(), FakeIntroRepository())

            assertNull(consent.missingFor(VerificationMethod.WAKE))
        }

    @Test
    fun `받은 적 없는 항목에 동의하면 현행 약관 버전으로 기록한다`() =
        runBlocking {
            // 조회 응답에 버전이 없어 그대로 보내면 서버가 버전 불일치로 막는다.
            val account = FakeAccountRepository()

            SensitiveConsent(account, FakeIntroRepository()).agree(AgreementType.LOCATION_INFO)

            val sent = account.submitted.single()
            assertEquals(AgreementType.LOCATION_INFO, sent.type)
            assertEquals("1.0", sent.version)
        }
}
