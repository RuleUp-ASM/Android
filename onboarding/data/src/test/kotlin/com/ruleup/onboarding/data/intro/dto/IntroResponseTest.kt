package com.ruleup.onboarding.data.intro.dto

import com.ruleup.domain.entity.user.AgreementType
import com.ruleup.domain.entity.user.TermsVersions
import kotlin.test.Test
import kotlin.test.assertEquals

class IntroResponseTest {
    @Test
    fun `비어 있는 약관 버전은 기본 버전을 사용한다`() {
        val versions = TermsVersionsResponse(termsOfService = "", privacyPolicy = "  ", marketing = "2.0").toDomain()
        assertEquals(TermsVersions.FALLBACK_VERSION, versions.of(AgreementType.TERMS_OF_SERVICE))
        assertEquals(TermsVersions.FALLBACK_VERSION, versions.of(AgreementType.PRIVACY_POLICY))
        assertEquals("2.0", versions.of(AgreementType.MARKETING))
    }
}
