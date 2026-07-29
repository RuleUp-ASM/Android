package com.ruleup.onboarding.data.profile.repository

import com.ruleup.network.dto.throwOnError
import com.ruleup.onboarding.data.profile.api.OnboardingProfileApi
import com.ruleup.onboarding.data.profile.dto.OnboardingMeRequest
import com.ruleup.onboarding.domain.profile.OnboardingProfileRepository
import javax.inject.Inject

class OnboardingProfileRepositoryImpl
    @Inject
    constructor(
        private val api: OnboardingProfileApi,
    ) : OnboardingProfileRepository {
        override suspend fun updateOnboardingInfo(
            birthDate: String?,
            gender: String?,
        ) {
            api.updateOnboardingMe(OnboardingMeRequest(birthDate = birthDate, gender = gender)).throwOnError()
        }
    }
