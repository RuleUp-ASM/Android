package com.ruleup.onboarding.domain.auth.repository

import com.ruleup.onboarding.domain.entity.DeviceIdentity

/**
 * 기기·설치 식별자를 제공한다. 최초 1회 만들어 영속하고 이후에는 같은 값을 돌려준다.
 *
 * 소비자가 온보딩뿐이라 core 가 아니라 여기에 둔다 — 알림 모듈의 `POST /devices` 는 FCM 토큰과
 * 플랫폼만 받고 `deviceId` 를 쓰지 않는다.
 */
interface DeviceIdentityRepository {
    suspend fun current(): DeviceIdentity
}
