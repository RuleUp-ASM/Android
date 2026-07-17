package com.ruleup.profile.data.repository

import com.ruleup.network.dto.getOrThrow
import com.ruleup.profile.data.api.MyPageApi
import com.ruleup.profile.data.dto.toDomain
import com.ruleup.profile.data.dto.toGroupChallenges
import com.ruleup.profile.domain.entity.GroupChallengeSummary
import com.ruleup.profile.domain.entity.MyHome
import com.ruleup.profile.domain.repository.MyPageRepository
import javax.inject.Inject

class MyPageRepositoryImpl
    @Inject
    constructor(
        private val api: MyPageApi,
    ) : MyPageRepository {
        override suspend fun getHome(): MyHome =
            api
                .getHome()
                .getOrThrow()
                .toDomain()

        override suspend fun getMyGroupChallenges(): List<GroupChallengeSummary> =
            api
                .getMyChallenges()
                .getOrThrow()
                .toGroupChallenges()
    }
