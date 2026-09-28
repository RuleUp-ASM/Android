package com.ruleup.onboarding.domain.intro.repository

import com.ruleup.domain.entity.user.TermsVersions
import com.ruleup.onboarding.domain.intro.entity.IntroInfo

/** 앱 진입 정보 조회(GET /v1/intro). */
interface IntroRepository {
    suspend fun getIntro(): IntroInfo

    /** 마지막 조회에서 받은 약관 버전. */
    fun lastTermsVersions(): TermsVersions
}
