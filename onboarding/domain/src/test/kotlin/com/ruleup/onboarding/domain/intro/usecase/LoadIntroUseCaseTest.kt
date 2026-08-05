package com.ruleup.onboarding.domain.intro.usecase

import com.ruleup.onboarding.domain.entity.AgreementType
import com.ruleup.onboarding.domain.entity.AppVersionGate
import com.ruleup.onboarding.domain.entity.IntroInfo
import com.ruleup.onboarding.domain.entity.TermsVersions
import com.ruleup.onboarding.domain.fake.FakeIntroRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LoadIntroUseCaseTest {
    @Test
    fun `조회에 성공하면 버전 게이트와 약관 버전을 그대로 반환한다`() =
        runBlocking {
            val info =
                IntroInfo(
                    versionGate = AppVersionGate(forceUpdate = true, devTestMsg = null, minAppVersion = "1.0.0"),
                    termsVersions = TermsVersions(mapOf(AgreementType.TERMS_OF_SERVICE to "1.2")),
                )
            val intro = FakeIntroRepository().apply { result = info }

            assertEquals(info, LoadIntroUseCase(intro)())
        }

    @Test
    fun `조회가 실패하면 페일오픈으로 null 을 반환한다`() =
        runBlocking {
            // 버전 점검 API 장애가 앱 실행 자체를 막으면 안 된다.
            val intro = FakeIntroRepository().apply { error = RuntimeException("network") }

            assertNull(LoadIntroUseCase(intro)())
        }

    @Test
    fun `약관 버전이 비어 오면 폴백 버전으로 떨어진다`() {
        val versions = TermsVersions(emptyMap())

        assertEquals(TermsVersions.FALLBACK_VERSION, versions.of(AgreementType.PRIVACY_POLICY))
    }
}
