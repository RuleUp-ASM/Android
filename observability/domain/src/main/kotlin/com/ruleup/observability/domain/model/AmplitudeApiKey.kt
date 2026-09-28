package com.ruleup.observability.domain.model

/** Amplitude 수집 키. */
data class AmplitudeApiKey(
    val value: String,
) {
    val isConfigured: Boolean get() = value.isNotBlank()
}
