package com.ruleup.support.domain.repository

import com.ruleup.support.domain.entity.InquiryDeviceContext

/**
 * 접수에 자동으로 붙는 진단 정보 채집 포트(driven adapter).
 *
 * 앱 버전·OS 버전·기기 모델은 Android API 의존이라 data 가 구현한다. **토글이 없다** — 고지 후
 * 필수로 붙이는 값이고, 못 채운 항목은 null 로 둔 채 접수를 진행한다.
 */
fun interface DeviceContextProvider {
    suspend fun capture(): InquiryDeviceContext
}
