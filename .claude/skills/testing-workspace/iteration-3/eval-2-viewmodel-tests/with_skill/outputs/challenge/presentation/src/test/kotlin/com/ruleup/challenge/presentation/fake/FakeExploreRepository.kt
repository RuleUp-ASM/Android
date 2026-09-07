package com.ruleup.challenge.presentation.fake

import com.ruleup.challenge.domain.entity.ExploreChallenge
import com.ruleup.challenge.domain.entity.ExploreFilter
import com.ruleup.challenge.domain.entity.ExploreResult
import com.ruleup.challenge.domain.entity.ExploreSort
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.challenge.domain.repository.ExploreRepository
import com.ruleup.domain.entity.category.Category
import kotlinx.coroutines.CompletableDeferred

/**
 * 테스트용 [ExploreRepository]. 둘러보기만 응답하고 나머지는 호출되면 실패한다 —
 * ViewModel 이 의도치 않은 API 를 부르면 조용히 지나가지 않고 드러나게 하려는 것이다.
 *
 * [responses] 는 **호출 순서대로** 소비된다. 재조회(정렬 되돌리기·커서 복구)가 몇 번째 호출인지가
 * 곧 검증 대상이라, 한 번의 인텐트가 두 번 조회하는 경로를 응답 목록으로 그대로 표현한다.
 * 목록이 바닥나면 마지막 응답을 반복한다.
 */
class FakeExploreRepository(
    private val responses: List<Result<ExploreResult>> = listOf(Result.success(exploreResult())),
) : ExploreRepository {
    init {
        require(responses.isNotEmpty()) { "응답을 하나 이상 줘야 한다" }
    }

    /** 서버에 실제로 나간 조건. 첫 페이지인지(cursor == null) 이어붙이기인지가 여기서 갈린다. */
    data class Query(
        val filter: ExploreFilter,
        val sort: ExploreSort,
        val cursor: String?,
    )

    val queries = mutableListOf<Query>()

    /** 채우면 응답이 여기서 멈춘다. "진행 중 재진입"을 재현할 때만 쓴다. */
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun explore(
        filter: ExploreFilter,
        sort: ExploreSort,
        cursor: String?,
        size: Int?,
    ): ExploreResult {
        val index = queries.size
        queries += Query(filter = filter, sort = sort, cursor = cursor)
        gate?.await()
        return responses.getOrElse(index) { responses.last() }.getOrThrow()
    }

    override suspend fun getTrending(category: Category?) = throw NotImplementedError()

    override suspend fun getCategories() = throw NotImplementedError()

    override suspend fun clone(challengeId: String) = throw NotImplementedError()
}

/**
 * 카드 한 장. 목록의 정체성은 [challengeId] 뿐이라 나머지는 기본값으로 두고,
 * 노출 이벤트가 싣는 축([isFull]·[eligible]·지표 유무)만 흔들 수 있게 연다.
 */
fun exploreChallenge(
    challengeId: String,
    isFull: Boolean = false,
    eligible: Boolean = true,
    completionRate: Double? = null,
) = ExploreChallenge(
    challengeId = challengeId,
    title = "챌린지 $challengeId",
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

/** [nextCursor] 가 null 이면 마지막 페이지다. */
fun exploreResult(
    vararg challengeIds: String,
    nextCursor: String? = null,
) = ExploreResult(
    items = challengeIds.map { exploreChallenge(it) },
    nextCursor = nextCursor,
    hasNext = nextCursor != null,
)
