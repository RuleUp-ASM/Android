package com.ruleup.challenge.presentation.fake

import com.ruleup.challenge.domain.entity.ChallengeCategoryCount
import com.ruleup.challenge.domain.entity.DraftResult
import com.ruleup.challenge.domain.entity.ExploreFilter
import com.ruleup.challenge.domain.entity.ExploreResult
import com.ruleup.challenge.domain.entity.ExploreSort
import com.ruleup.challenge.domain.entity.TrendingSnapshot
import com.ruleup.challenge.domain.repository.ExploreRepository
import com.ruleup.domain.entity.category.Category

/** 테스트용 [ExploreRepository]. 준비하지 않은 메서드는 호출되면 실패한다. */
class FakeExploreRepository(
    private val trending: (() -> TrendingSnapshot)? = null,
    private val categories: (() -> List<ChallengeCategoryCount>)? = null,
    private val explore: ((ExploreFilter, ExploreSort, String?) -> ExploreResult)? = null,
    private val clone: ((String) -> DraftResult.Ok)? = null,
) : ExploreRepository {
    val calls = mutableListOf<String>()

    override suspend fun getTrending(category: Category?): TrendingSnapshot {
        calls += "getTrending"
        return requireNotNull(trending) { "getTrending 을 준비하지 않았다" }()
    }

    override suspend fun getCategories(): List<ChallengeCategoryCount> {
        calls += "getCategories"
        return requireNotNull(categories) { "getCategories 를 준비하지 않았다" }()
    }

    /** 어떤 조건으로 몇 번 물었는지. 자가 복구 경로에서 조건이 바뀌는지 보려면 이게 있어야 한다. */
    val exploreQueries = mutableListOf<Triple<ExploreFilter, ExploreSort, String?>>()

    override suspend fun explore(
        filter: ExploreFilter,
        sort: ExploreSort,
        cursor: String?,
        size: Int?,
    ): ExploreResult {
        calls += "explore"
        exploreQueries += Triple(filter, sort, cursor)
        return requireNotNull(explore) { "explore 를 준비하지 않았다" }(filter, sort, cursor)
    }

    override suspend fun clone(challengeId: String): DraftResult.Ok {
        calls += "clone"
        return requireNotNull(clone) { "clone 을 준비하지 않았다" }(challengeId)
    }
}
