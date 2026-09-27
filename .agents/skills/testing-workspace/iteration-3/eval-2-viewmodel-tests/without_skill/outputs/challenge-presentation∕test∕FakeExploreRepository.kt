package com.ruleup.challenge.presentation.fake

import com.ruleup.challenge.domain.entity.ExploreFilter
import com.ruleup.challenge.domain.entity.ExploreResult
import com.ruleup.challenge.domain.entity.ExploreSort
import com.ruleup.challenge.domain.repository.ExploreRepository
import com.ruleup.domain.entity.category.Category
import kotlinx.coroutines.CompletableDeferred

/** [ExploreRepository.explore] 호출 하나. 화면이 **무엇을 물어봤는지**가 검증 대상이라 인자를 그대로 남긴다. */
data class ExploreCall(
    val filter: ExploreFilter,
    val sort: ExploreSort,
    val cursor: String?,
    val size: Int?,
)

/**
 * 테스트용 [ExploreRepository].
 *
 * [outcomes] 를 호출 순서대로 소비하고 동나면 [fallback] 을 돌려준다 — 서버가 조건을 거절하면 화면이
 * 스스로 고쳐 **다시 조회**하므로, 한 인텐트가 두 번 이상 호출을 만든다. 순서대로 답이 달라져야
 * 그 재조회를 검증할 수 있다.
 *
 * 검증 대상이 아닌 메서드는 호출되면 실패한다 — 목록 화면이 인기·카테고리·복제를 건드리면 드러나게 한다.
 */
class FakeExploreRepository(
    outcomes: List<Result<ExploreResult>> = emptyList(),
    private val fallback: Result<ExploreResult> = Result.success(ExploreResult(emptyList(), null, false)),
) : ExploreRepository {
    val calls = mutableListOf<ExploreCall>()

    /** 응답을 붙잡아 두는 게이트. 진행 중 상태(로딩·중복 호출 차단)를 볼 때만 채운다. */
    var gate: CompletableDeferred<Unit>? = null

    private val queue = ArrayDeque(outcomes)

    override suspend fun explore(
        filter: ExploreFilter,
        sort: ExploreSort,
        cursor: String?,
        size: Int?,
    ): ExploreResult {
        calls += ExploreCall(filter = filter, sort = sort, cursor = cursor, size = size)
        gate?.await()
        return (queue.removeFirstOrNull() ?: fallback).getOrThrow()
    }

    override suspend fun getTrending(category: Category?) = throw NotImplementedError()

    override suspend fun getCategories() = throw NotImplementedError()

    override suspend fun clone(challengeId: String) = throw NotImplementedError()
}
