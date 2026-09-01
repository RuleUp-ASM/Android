package com.ruleup.challenge.presentation.fake

import com.ruleup.challenge.domain.entity.ExploreChallenge
import com.ruleup.challenge.domain.entity.ExploreFilter
import com.ruleup.challenge.domain.entity.ExploreResult
import com.ruleup.challenge.domain.entity.ExploreSort
import com.ruleup.challenge.domain.repository.ExploreRepository
import com.ruleup.domain.entity.category.Category
import kotlinx.coroutines.CompletableDeferred

/**
 * 테스트용 [ExploreRepository]. 응답을 **호출 순서대로 미리 깔아두고**([succeed]·[fail]) 실제로 어떤
 * 조건으로 불렸는지를 [calls] 에 남긴다 — 둘러보기는 "무엇을 받았나"만큼 "무엇을 보냈나"(커서·필터·정렬)가
 * 계약이라, 반환값만 흉내 내는 대역으로는 검증이 반쪽이 된다.
 *
 * 검증 대상이 아닌 메서드는 호출되면 실패한다. 의도치 않은 호출이 조용히 지나가지 않게 하려는 것이다.
 */
class FakeExploreRepository : ExploreRepository {
    data class Call(
        val filter: ExploreFilter,
        val sort: ExploreSort,
        val cursor: String?,
    )

    private val scripted = ArrayDeque<Result<ExploreResult>>()
    private val recorded = mutableListOf<Call>()

    val calls: List<Call> get() = recorded.toList()

    /**
     * 채우면 다음 [explore] 한 번이 이 신호를 기다린다. 요청이 떠 있는 동안의 중복 호출을 재현하는
     * 유일한 방법이라, 로딩 가드 테스트에서만 쓴다.
     */
    var gate: CompletableDeferred<Unit>? = null

    fun succeed(
        items: List<ExploreChallenge> = emptyList(),
        nextCursor: String? = null,
    ) {
        scripted += Result.success(ExploreResult(items = items, nextCursor = nextCursor, hasNext = nextCursor != null))
    }

    fun fail(error: Throwable) {
        scripted += Result.failure(error)
    }

    /** 깔아둔 응답이 떨어지면 빈 페이지를 준다. 과다 호출은 [calls] 크기로 잡는다. */
    override suspend fun explore(
        filter: ExploreFilter,
        sort: ExploreSort,
        cursor: String?,
        size: Int?,
    ): ExploreResult {
        recorded += Call(filter = filter, sort = sort, cursor = cursor)
        val pending = gate
        gate = null
        pending?.await()
        return (scripted.removeFirstOrNull() ?: Result.success(EMPTY_PAGE)).getOrThrow()
    }

    override suspend fun getTrending(category: Category?) = throw NotImplementedError()

    override suspend fun getCategories() = throw NotImplementedError()

    override suspend fun clone(challengeId: String) = throw NotImplementedError()

    private companion object {
        val EMPTY_PAGE = ExploreResult(items = emptyList(), nextCursor = null, hasNext = false)
    }
}
