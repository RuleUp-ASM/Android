package com.ruleup.observability.domain.model

/**
 * Amplitude 수집 키.
 *
 * [BuildProfile] 과 같은 이유로 **`:app` 이 제공한다** — 라이브러리 모듈은 앱의 `BuildConfig` 를
 * 볼 수 없다. 값은 `local.properties` 에서 주입되므로 저장소에 남지 않는다.
 *
 * 비어 있을 수 있다(키를 아직 안 넣은 개발 환경). 그때 출구를 조용히 붙였다가는 이벤트가
 * 어디로도 가지 않으면서 아무도 모르게 되므로, 배선 쪽에서 [isConfigured] 로 판단해 아예 달지 않는다.
 *
 * `@JvmInline value class` 로 두지 않는다 — 인라인 클래스를 받는 Hilt 프로바이더는 이름이
 * 뭉개져(`sink-QAYGdyI`) KSP 가 "유효한 이름이 아니다"로 실패한다.
 */
data class AmplitudeApiKey(
    val value: String,
) {
    val isConfigured: Boolean get() = value.isNotBlank()
}
