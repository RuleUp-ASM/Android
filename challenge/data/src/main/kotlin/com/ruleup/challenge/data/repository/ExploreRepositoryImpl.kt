package com.ruleup.challenge.data.repository

import com.ruleup.challenge.data.api.ChallengeApi
import com.ruleup.challenge.data.dto.toDomain
import com.ruleup.challenge.domain.entity.ChallengeCategoryCount
import com.ruleup.challenge.domain.entity.ExploreFilter
import com.ruleup.challenge.domain.entity.ExploreResult
import com.ruleup.challenge.domain.entity.ExploreSort
import com.ruleup.challenge.domain.entity.TrendingChallenge
import com.ruleup.challenge.domain.repository.ExploreRepository
import com.ruleup.entity.user.InterestCategory
import com.ruleup.network.dto.getOrThrow
import javax.inject.Inject

class ExploreRepositoryImpl
    @Inject
    constructor(
        private val api: ChallengeApi,
    ) : ExploreRepository {
        override suspend fun getTrending(category: InterestCategory?): List<TrendingChallenge> =
            api
                .getTrending(category?.value)
                .getOrThrow()
                .toDomain()

        override suspend fun getCategories(): List<ChallengeCategoryCount> =
            api
                .getCategories()
                .getOrThrow()
                .toDomain()

        override suspend fun explore(
            filter: ExploreFilter,
            sort: ExploreSort,
            cursor: String?,
            size: Int?,
        ): ExploreResult =
            api
                .explore(
                    category = filter.category?.value,
                    participationType = filter.participationType?.value,
                    verificationMethod = filter.verificationMethod?.value,
                    mannerCut = filter.mannerCut,
                    sort = sort.value,
                    cursor = cursor,
                    size = size,
                ).getOrThrow()
                .toDomain()
    }
