package com.ruleup.onboarding.data.intro.repository

import com.ruleup.domain.device.DeviceInfoProvider
import com.ruleup.domain.entity.user.TermsVersions
import com.ruleup.network.dto.getOrThrow
import com.ruleup.onboarding.data.intro.api.IntroApi
import com.ruleup.onboarding.data.intro.dto.toDomain
import com.ruleup.onboarding.domain.intro.entity.IntroInfo
import com.ruleup.onboarding.domain.intro.repository.IntroRepository
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

class IntroRepositoryImpl
    @Inject
    constructor(
        private val api: IntroApi,
        private val deviceInfoProvider: DeviceInfoProvider,
    ) : IntroRepository {
        // 성공한 조회의 약관 버전만 남긴다.
        private val termsVersions = AtomicReference<TermsVersions?>(null)

        override suspend fun getIntro(): IntroInfo {
            val deviceInfo = deviceInfoProvider.current()
            return api
                .getIntro(
                    appVersionCode = deviceInfo.versionCode,
                    platform = deviceInfo.platform,
                ).getOrThrow()
                .toDomain()
                .also { termsVersions.set(it.termsVersions) }
        }

        override fun lastTermsVersions(): TermsVersions = termsVersions.get() ?: TermsVersions(emptyMap())
    }
