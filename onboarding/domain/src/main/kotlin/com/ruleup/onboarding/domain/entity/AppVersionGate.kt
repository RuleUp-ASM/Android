package com.ruleup.onboarding.domain.entity

/**
 * 앱 버전 게이트 판정 결과(GET /v1/intro). 스플래시에서 강제/권장 업데이트를 안내하는 데 쓴다.
 *
 * @property forceUpdate true 면 더 진행하지 않고 강제 업데이트 화면을 노출한다.
 * @property devTestMsg 개발/점검용 안내 메시지(없으면 null).
 * @property minAppVersion 지원하는 최소 앱 버전명(예 "1.0.0").
 * @property recommendAppVersion 권장 앱 버전명(소프트 업데이트 유도용, 예 "1.2.0").
 */
data class AppVersionGate(
    val forceUpdate: Boolean,
    val devTestMsg: String?,
    val minAppVersion: String?,
    val recommendAppVersion: String?,
)
