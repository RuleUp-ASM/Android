package com.ruleup.logging.domain

/**
 * 전송 배선이 필요로 하는 앱 단위 값. **`:app` 이 제공한다** — 라이브러리 모듈은 앱의
 * `BuildConfig` 를 볼 수 없다.
 *
 * @param amplitudeApiKey Amplitude 수집 키. 비어 있으면 그 출구를 아예 달지 않는다 — 키 없이 SDK 를
 *   띄워 조용히 실패하는 것보다, 안 붙어서 로그에 안 보이는 편이 원인 추적이 빠르다.
 * @param debuggable 개발자에게 더 보여줘도 되는 빌드인가. Logcat 출구와 SDK 상세 로그의 기준이다.
 */
data class BizLogConfig(
    val amplitudeApiKey: String,
    val debuggable: Boolean,
) {
    val isAmplitudeConfigured: Boolean get() = amplitudeApiKey.isNotBlank()
}
