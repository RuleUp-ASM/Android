package com.ruleup.domain.device

/** 기기와 앱 버전 정보. */
data class DeviceInfo(
    val platform: String,
    val osVersion: String,
    val sdkInt: Int,
    val deviceModel: String,
    val manufacturer: String,
    val lowRam: Boolean,
    val versionName: String,
    val versionCode: Int,
)
