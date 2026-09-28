package com.ruleup.domain.device

/** 기기 정보 조회. */
fun interface DeviceInfoProvider {
    fun current(): DeviceInfo
}
