package com.ruleup.observability.domain.model

/**
 * Amplitude 수집 키. [BuildProfile] 과 같은 이유로 **`:app` 이 `local.properties` 값으로 제공한다** —
 * 라이브러리 모듈은 앱의 `BuildConfig` 를 볼 수 없다. 키를 안 넣은 개발 환경에서는 비어 있다.
 *
 * `@JvmInline value class` 로 두지 않는다 — 인라인 클래스를 받는 Hilt 프로바이더는 이름이
 * 뭉개져(`sink-QAYGdyI`) KSP 가 실패한다.
 */
data class AmplitudeApiKey(
    val value: String,
) {
    val isConfigured: Boolean get() = value.isNotBlank()
}
