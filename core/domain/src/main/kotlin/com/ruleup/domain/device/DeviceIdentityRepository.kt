package com.ruleup.domain.device

/** 기기·설치 식별자 저장소. */
interface DeviceIdentityRepository {
    suspend fun current(): DeviceIdentity
}
