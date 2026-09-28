package com.ruleup.support.data.repository

import com.ruleup.domain.device.DeviceInfoProvider
import com.ruleup.support.domain.entity.InquiryDeviceContext
import com.ruleup.support.domain.repository.DeviceContextProvider
import javax.inject.Inject

/** 접수에 자동으로 붙는 진단 정보 채집. */
class DeviceContextProviderImpl
    @Inject
    constructor(
        private val deviceInfoProvider: DeviceInfoProvider,
    ) : DeviceContextProvider {
        override suspend fun capture(): InquiryDeviceContext {
            val device = runCatching { deviceInfoProvider.current() }.getOrNull()
            return InquiryDeviceContext(
                appVersion = device?.versionName,
                osVersion = device?.osVersion?.let { "Android $it".trim() },
                deviceModel = device?.deviceModel,
                // 최근 오류 로그 ID 를 앱이 보관하는 자리가 아직 없다.
                errorLogId = null,
            )
        }
    }
