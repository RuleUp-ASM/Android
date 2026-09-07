package com.ruleup.challenge.presentation.fake

import com.ruleup.challenge.domain.entity.ChallengeCategoryCount
import com.ruleup.challenge.domain.entity.DraftResult
import com.ruleup.challenge.domain.entity.ExploreChallenge
import com.ruleup.challenge.domain.entity.ExploreFilter
import com.ruleup.challenge.domain.entity.ExploreResult
import com.ruleup.challenge.domain.entity.ExploreSort
import com.ruleup.challenge.domain.entity.TrendingSnapshot
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.challenge.domain.repository.ExploreRepository
import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.entity.user.Tier
import kotlinx.coroutines.CompletableDeferred

/** 서버로 나간 조회 조건 한 번. 필터·정렬·커서가 실제로 무엇으로 나갔는지가 검증 대상이다. */
data class ExploreCall(
    val filter: ExploreFilter,
    val sort: ExploreSort,
    val cursor: String?,
)

/**
 * 둘러보기 조회 대역.
 *
 * 응답을 **호출 순서대로** 큐에 넣는다 — 이 화면은 한 인텐트가 실패 → 조건 교정 → 재조회로 두 번
 * 이상 나가는 경로가 많아서, 호출마다 다른 답을 줄 수 없으면 자기 복구를 재현할 수 없다.
 * 큐가 비면 [alwaysAnswer] 로 지정한 기본 응답이 계속 나온다.
 */
class FakeExploreRepository : ExploreRepository {
    val calls = mutableListOf<ExploreCall>()

    private val queued = ArrayDeque<() -> ExploreResult>()
    private var default: () -> ExploreResult = { exploreResult() }

    /**
     * 채워 두면 응답이 이 신호를 기다린다. "요청이 아직 안 끝난" 상태를 붙잡아 두는 유일한 수단이라
     * 중복 호출 차단은 이걸로만 검증할 수 있다.
     */
    var gate: CompletableDeferred<Unit>? = null

    fun enqueue(vararg answers: () -> ExploreResult) {
        queued.addAll(answers.toList())
    }

    fun alwaysAnswer(answer: () -> ExploreResult) {
        default = answer
    }

    override suspend fun explore(
        filter: ExploreFilter,
        sort: ExploreSort,
        cursor: String?,
        size: Int?,
    ): ExploreResult {
        calls += ExploreCall(filter = filter, sort = sort, cursor = cursor)
        gate?.await()
        return (queued.removeFirstOrNull() ?: default).invoke()
    }

    override suspend fun getTrending(category: Category?): TrendingSnapshot = notUsed()

    override suspend fun getCategories(): List<ChallengeCategoryCount> = notUsed()

    override suspend fun clone(challengeId: String): DraftResult.Ok = notUsed()

    private fun notUsed(): Nothing = error("둘러보기 목록 화면이 쓰지 않는 계약이다")
}

/** 응답 대신 예외를 내는 답. 서버 거절 코드별 자기 복구 경로를 태울 때 쓴다. */
fun throws(error: Throwable): () -> ExploreResult = { throw error }

fun exploreResult(
    items: List<ExploreChallenge> = emptyList(),
    nextCursor: String? = null,
): ExploreResult = ExploreResult(items = items, nextCursor = nextCursor, hasNext = nextCursor != null)

fun answer(
    items: List<ExploreChallenge> = emptyList(),
    nextCursor: String? = null,
): () -> ExploreResult = { exploreResult(items = items, nextCursor = nextCursor) }

/** 카드 하나. 검증에 쓰지 않는 필드는 기본값으로 덮어 두고 테스트가 관심 있는 축만 지정한다. */
fun exploreChallenge(
    challengeId: String = "c1",
    title: String = "제목",
    category: Category? = Category.EXERCISE,
    startsSoon: Boolean = false,
    isFull: Boolean = false,
    eligible: Boolean = true,
    completionRate: Double? = 0.7,
    retentionRate: Double? = 0.5,
    minTier: Tier? = null,
): ExploreChallenge =
    ExploreChallenge(
        challengeId = challengeId,
        title = title,
        imageUrl = null,
        category = category,
        verificationType = VerificationType.AUTO,
        startsSoon = startsSoon,
        participantCount = 3,
        capacity = 10,
        isFull = isFull,
        minTier = minTier,
        eligible = eligible,
        completionRate = completionRate,
        retentionRate = retentionRate,
        dday = 7,
        startDate = "2026-08-01",
        endDate = "2026-09-01",
        createdAt = "2026-07-30T00:00:00+09:00",
    )

fun challenges(vararg ids: String): List<ExploreChallenge> = ids.map { exploreChallenge(challengeId = it) }
