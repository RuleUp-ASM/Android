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
 * 테스트용 [ExploreRepository]. 검증 대상인 [explore] 만 답하고 나머지는 호출되면 실패한다 —
 * 목록 화면이 의도치 않은 API 를 부르면 조용히 지나가지 않게 하려는 것이다.
 *
 * 응답을 **큐로** 받는 이유는 이 ViewModel 이 한 인텐트에 두 번 조회하는 경로(정렬·필터 거절 후
 * 자가 복구, 커서 손상 후 첫 페이지 재요청)를 갖고 있어서다. "첫 호출은 거절, 두 번째는 성공"을
 * 그대로 적을 수 있어야 복구가 실제로 도는지 볼 수 있다. 큐가 비면 [fallback] 을 돌려준다 —
 * 자가 복구가 무한히 되풀이돼도 테스트가 멈추지 않고 [calls] 로 드러나게 하려는 것이다.
 */
class FakeExploreRepository(
    responses: List<Result<ExploreResult>> = emptyList(),
    private val fallback: ExploreResult = exploreResult(),
) : ExploreRepository {
    data class Call(
        val filter: ExploreFilter,
        val sort: ExploreSort,
        val cursor: String?,
    )

    private val queue = ArrayDeque(responses)

    val calls = mutableListOf<Call>()

    /** 채우면 이후 [explore] 가 완료될 때까지 멈춘다. 응답 도착 전 재요청(연타)을 재현하는 용도. */
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun explore(
        filter: ExploreFilter,
        sort: ExploreSort,
        cursor: String?,
        size: Int?,
    ): ExploreResult {
        calls += Call(filter = filter, sort = sort, cursor = cursor)
        gate?.await()
        return (queue.removeFirstOrNull() ?: Result.success(fallback)).getOrThrow()
    }

    override suspend fun getTrending(category: Category?) = throw NotImplementedError()

    override suspend fun getCategories() = throw NotImplementedError()

    override suspend fun clone(challengeId: String) = throw NotImplementedError()
}

fun exploreResult(
    items: List<ExploreChallenge> = emptyList(),
    nextCursor: String? = null,
) = ExploreResult(items = items, nextCursor = nextCursor, hasNext = nextCursor != null)

fun exploreChallenge(
    challengeId: String = "c1",
    isFull: Boolean = false,
    eligible: Boolean = true,
    startsSoon: Boolean = false,
    completionRate: Double? = 0.8,
) = ExploreChallenge(
    challengeId = challengeId,
    title = "매일 아침 6시 기상",
    imageUrl = null,
    category = Category.WAKE_SLEEP,
    verificationType = VerificationType.MANUAL,
    startsSoon = startsSoon,
    participantCount = 10,
    capacity = 50,
    isFull = isFull,
    minTier = null,
    eligible = eligible,
    completionRate = completionRate,
    retentionRate = null,
    dday = 7,
    startDate = "2026-08-12",
    endDate = "2026-08-26",
    createdAt = "2026-08-11T10:00:00+09:00",
)
