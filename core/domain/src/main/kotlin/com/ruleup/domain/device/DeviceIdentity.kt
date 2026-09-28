package com.ruleup.domain.device

/** 로그인·가입 요청에 함께 실리는 두 식별자. */
data class DeviceIdentity(
    val deviceId: String,
    val installationId: String,
)
