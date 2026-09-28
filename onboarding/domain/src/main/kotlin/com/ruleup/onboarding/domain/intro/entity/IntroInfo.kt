package com.ruleup.onboarding.domain.intro.entity

import com.ruleup.domain.entity.user.TermsVersions

/** 앱 진입 시 가장 먼저 받는 정보(GET /v1/intro). */
data class IntroInfo(
    val versionGate: AppVersionGate,
    val termsVersions: TermsVersions,
)

/** 앱 버전 게이트. */
data class AppVersionGate(
    val forceUpdate: Boolean,
    val devTestMsg: String?,
    val minAppVersion: String?,
)
