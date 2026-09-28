package com.ruleup.logging.domain

/** 전송 배선이 필요로 하는 앱 단위 값. */
data class BizLogConfig(
    val amplitudeApiKey: String,
    val debuggable: Boolean,
) {
    val isAmplitudeConfigured: Boolean get() = amplitudeApiKey.isNotBlank()
}
