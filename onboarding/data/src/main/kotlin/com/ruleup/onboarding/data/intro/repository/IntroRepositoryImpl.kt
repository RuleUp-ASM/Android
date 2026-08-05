package com.ruleup.onboarding.data.intro.repository

import com.ruleup.network.dto.getOrThrow
import com.ruleup.onboarding.data.device.DeviceInfoProvider
import com.ruleup.onboarding.data.intro.api.IntroApi
import com.ruleup.onboarding.data.intro.dto.toDomain
import com.ruleup.onboarding.domain.entity.IntroInfo
import com.ruleup.onboarding.domain.intro.repository.IntroRepository
import javax.inject.Inject

class IntroRepositoryImpl
    @Inject
    constructor(
        private val api: IntroApi,
        private val deviceInfoProvider: DeviceInfoProvider,
    ) : IntroRepository {
        override suspend fun getIntro(): IntroInfo {
            val deviceInfo = deviceInfoProvider.current()
            return api
                .getIntro(
                    appVersionCode = deviceInfo.versionCode ?: 0,
                    platform = deviceInfo.platform,
                ).getOrThrow()
                .toDomain()
        }
    }
