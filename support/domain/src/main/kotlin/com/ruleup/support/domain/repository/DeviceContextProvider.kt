package com.ruleup.support.domain.repository

import com.ruleup.support.domain.entity.InquiryDeviceContext

/** 접수에 자동으로 붙는 진단 정보 채집 포트(driven adapter). */
fun interface DeviceContextProvider {
    suspend fun capture(): InquiryDeviceContext
}
