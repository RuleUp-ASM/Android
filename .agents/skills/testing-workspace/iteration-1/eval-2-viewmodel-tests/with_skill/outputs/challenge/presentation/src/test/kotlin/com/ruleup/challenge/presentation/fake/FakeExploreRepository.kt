package com.ruleup.challenge.presentation.fake

import com.ruleup.challenge.domain.entity.ExploreChallenge
import com.ruleup.challenge.domain.entity.ExploreFilter
import com.ruleup.challenge.domain.entity.ExploreResult
import com.ruleup.challenge.domain.entity.ExploreSort
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.challenge.domain.repository.ExploreRepository
import com.ruleup.domain.entity.category.Category

/**
 * 테스트용 [ExploreRepository].
 *
 * 응답을 **호출 순서대로** 소비한다 — 둘러보기는 실패 후 스스로 조건을 고쳐 다시 조회하므로, 한 번의
 * 인텐트가 몇 번 서버에 나갔는지와 그때 무엇을 들고 나갔는지가 곧 검증 대상이기 때문이다.
 * 준비한 응답보다 많이 부르면 실패한다 — 조용한 재요청 루프가 통과해 버리지 않게 한다.
 */
class FakeExploreRepository(
    vararg responses: () -> ExploreResult,
) : ExploreRepository {
    /** 서버로 나간 조회 조건. 인덱스가 곧 호출 순서다. */
    data class Call(
        val filter: ExploreFilter,
        val sort: ExploreSort,
        val cursor: String?,
    )

    private val remaining = ArrayDeque(responses.toList())
    val calls = mutableListOf<Call>()

    override suspend fun explore(
        filter: ExploreFilter,
        sort: ExploreSort,
        cursor: String?,
        size: Int?,
    ): ExploreResult {
        calls += Call(filter = filter, sort = sort, cursor = cursor)
        check(remaining.isNotEmpty()) { "준비한 응답보다 많이 조회했다: ${calls.size}번째 호출 $filter/$sort/$cursor" }
        return remaining.removeFirst().invoke()
    }

    override suspend fun getTrending(category: Category?) = throw NotImplementedError()

    override suspend fun getCategories() = throw NotImplementedError()

    override suspend fun clone(challengeId: String) = throw NotImplementedError()
}

/** 정상 응답 한 페이지. [nextCursor] 가 있으면 다음 페이지가 남은 것이다. */
fun page(
    vararg items: ExploreChallenge,
    nextCursor: String? = null,
): () -> ExploreResult = { ExploreResult(items = items.toList(), nextCursor = nextCursor, hasNext = nextCursor != null) }

/** 서버가 조회를 거절한 응답. */
fun fails(error: Throwable): () -> ExploreResult = { throw error }

/** 카드 하나. 이 층이 보는 건 id·순서·플래그뿐이라 나머지는 기본값으로 둔다. */
fun card(
    challengeId: String,
    isFull: Boolean = false,
    eligible: Boolean = true,
    completionRate: Double? = 0.7,
) = ExploreChallenge(
    challengeId = challengeId,
    title = challengeId,
    imageUrl = null,
    category = Category.EXERCISE,
    verificationType = VerificationType.MANUAL,
    startsSoon = false,
    participantCount = 3,
    capacity = 10,
    isFull = isFull,
    minTier = null,
    eligible = eligible,
    completionRate = completionRate,
    retentionRate = null,
    dday = 7,
    startDate = null,
    endDate = null,
    createdAt = null,
)
