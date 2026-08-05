package com.ruleup.onboarding.domain.entity

import com.ruleup.domain.entity.user.TermsVersions

/**
 * 앱 진입 시 가장 먼저 받는 정보(GET /v1/intro). 공개 엔드포인트라 토큰 없이 호출한다.
 *
 * 강제 업데이트 판정과 약관 버전이 한 응답에 함께 온다. 둘 다 로그인보다 먼저 필요해서다 —
 * 버전 게이트는 진입 자체를 막고, 약관 버전은 가입 화면이 하드코딩 없이 동의를 기록하게 한다.
 */
data class IntroInfo(
    val versionGate: AppVersionGate,
    val termsVersions: TermsVersions,
)

/**
 * 앱 버전 게이트. 업데이트 정책이 예외 없이 강제라 권장 버전 개념은 없다.
 *
 * @property forceUpdate true 면 더 진행하지 않고 강제 업데이트 화면을 노출한다.
 * @property devTestMsg 개발·점검용 안내 문구(없으면 null).
 * @property minAppVersion 지원하는 최소 앱 버전명(예 "1.0.0"). 안내 문구 표시용.
 */
data class AppVersionGate(
    val forceUpdate: Boolean,
    val devTestMsg: String?,
    val minAppVersion: String?,
)
