package com.ruleup.onboarding.domain.intro.usecase

import com.ruleup.domain.entity.user.AgreementType
import com.ruleup.domain.entity.user.TermsVersions
import com.ruleup.onboarding.domain.fake.FakeIntroRepository
import com.ruleup.onboarding.domain.intro.entity.AppVersionGate
import com.ruleup.onboarding.domain.intro.entity.IntroInfo
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class LoadIntroUseCaseTest {
    @Test
    fun `강제 업데이트면 안내에 쓸 값과 함께 게이트가 걸린다`() =
        runBlocking {
            val intro = repository(forceUpdate = true)

            assertEquals(
                IntroGate.ForceUpdate(minAppVersion = "1.0.0", devTestMsg = "점검 중"),
                LoadIntroUseCase(intro)(),
            )
        }

    @Test
    fun `강제 업데이트가 아니면 통과한다`() =
        runBlocking {
            val intro = repository(forceUpdate = false)

            assertEquals(IntroGate.Pass, LoadIntroUseCase(intro)())
        }

    @Test
    fun `조회가 실패하면 페일오픈으로 통과한다`() =
        runBlocking {
            // 버전 점검 API 장애가 앱 실행 자체를 막으면 안 된다.
            val intro = FakeIntroRepository().apply { error = RuntimeException("network") }

            assertEquals(IntroGate.Pass, LoadIntroUseCase(intro)())
        }

    @Test
    fun `조회에 성공하면 약관 버전이 남아 가입 화면이 꺼내 쓴다`() =
        runBlocking {
            val intro = repository(forceUpdate = false)
            LoadIntroUseCase(intro)()

            assertEquals("1.2", intro.lastTermsVersions().of(AgreementType.TERMS_OF_SERVICE))
        }

    @Test
    fun `조회 전이면 폴백 버전으로 떨어진다`() {
        // 페일오픈으로 인트로를 못 받아도 가입은 진행되어야 한다. 버전은 서버가 재검증한다.
        assertEquals(
            TermsVersions.FALLBACK_VERSION,
            FakeIntroRepository().lastTermsVersions().of(AgreementType.PRIVACY_POLICY),
        )
    }

    private fun repository(forceUpdate: Boolean) =
        FakeIntroRepository().apply {
            result =
                IntroInfo(
                    versionGate =
                        AppVersionGate(
                            forceUpdate = forceUpdate,
                            devTestMsg = "점검 중",
                            minAppVersion = "1.0.0",
                        ),
                    termsVersions = TermsVersions(mapOf(AgreementType.TERMS_OF_SERVICE to "1.2")),
                )
        }
}
