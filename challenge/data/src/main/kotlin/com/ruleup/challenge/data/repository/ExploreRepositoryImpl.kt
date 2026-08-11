package com.ruleup.challenge.data.repository

import com.ruleup.challenge.data.api.ChallengeApi
import com.ruleup.challenge.data.dto.toDomain
import com.ruleup.challenge.domain.entity.ChallengeCategoryCount
import com.ruleup.challenge.domain.entity.ChallengeNotCloneableException
import com.ruleup.challenge.domain.entity.ChallengeNotFoundException
import com.ruleup.challenge.domain.entity.CursorInvalidException
import com.ruleup.challenge.domain.entity.DraftResult
import com.ruleup.challenge.domain.entity.ExploreFilter
import com.ruleup.challenge.domain.entity.ExploreResult
import com.ruleup.challenge.domain.entity.ExploreSort
import com.ruleup.challenge.domain.entity.InvalidFilterValueException
import com.ruleup.challenge.domain.entity.InvalidSortTypeException
import com.ruleup.challenge.domain.entity.TrendingSnapshot
import com.ruleup.challenge.domain.repository.ExploreRepository
import com.ruleup.domain.entity.category.Category
import com.ruleup.network.dto.ApiException
import com.ruleup.network.dto.getOrThrow
import javax.inject.Inject

class ExploreRepositoryImpl
    @Inject
    constructor(
        private val api: ChallengeApi,
    ) : ExploreRepository {
        override suspend fun getTrending(category: Category?): TrendingSnapshot =
            api
                .getTrending(category = category?.value)
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
            try {
                api
                    .explore(
                        categories = filter.categoriesParam(),
                        verifyType = filter.verifyType?.value,
                        // 기본 off 라 꺼져 있으면 파라미터 자체를 보내지 않는다.
                        eligibleOnly = filter.eligibleOnly.takeIf { it },
                        sort = sort.value,
                        cursor = cursor,
                        size = size,
                    ).getOrThrow()
                    .toDomain()
            } catch (e: ApiException) {
                // 세 코드는 화면이 각각 다르게 자동 복구한다(기본 정렬 복귀 / 필터 초기화 / 첫 페이지 재요청).
                when (e.code) {
                    CODE_INVALID_SORT -> throw InvalidSortTypeException()
                    CODE_INVALID_FILTER -> throw InvalidFilterValueException()
                    CODE_CURSOR_INVALID -> throw CursorInvalidException()
                    else -> throw e
                }
            }

        override suspend fun clone(challengeId: String): DraftResult.Ok =
            try {
                api
                    .clone(challengeId)
                    .getOrThrow()
                    .toDomain()
            } catch (e: ApiException) {
                when (e.code) {
                    CODE_NOT_CLONEABLE -> throw ChallengeNotCloneableException()
                    CODE_CHALLENGE_NOT_FOUND -> throw ChallengeNotFoundException()
                    else -> throw e
                }
            }

        private companion object {
            const val CODE_INVALID_SORT = "INVALID_SORT_TYPE"
            const val CODE_INVALID_FILTER = "INVALID_FILTER_VALUE"
            const val CODE_CURSOR_INVALID = "CURSOR_INVALID"
            const val CODE_NOT_CLONEABLE = "NOT_CLONEABLE"
            const val CODE_CHALLENGE_NOT_FOUND = "CHALLENGE_NOT_FOUND"
        }
    }
