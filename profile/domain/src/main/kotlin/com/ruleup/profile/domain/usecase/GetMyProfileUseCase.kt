package com.ruleup.profile.domain.usecase

import com.ruleup.profile.domain.entity.MyProfile
import com.ruleup.profile.domain.repository.ProfileRepository
import javax.inject.Inject

/** 내 프로필 조회(GET /api/v1/users/me). */
class GetMyProfileUseCase
    @Inject
    constructor(
        private val profileRepository: ProfileRepository,
    ) {
        suspend operator fun invoke(): MyProfile = profileRepository.getMyProfile()
    }
