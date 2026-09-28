package com.ruleup.challenge.data.dto

import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.network.dto.ApiException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 탐색 응답 매핑. */
class ExploreResponseMappingTest {
    @Test
    fun `순위를 안 주면 서버가 내려준 순서로 매긴다`() {
        // 서버가 이미 정렬해 보내므로 배열 순서가 곧 순위다.
        val snapshot =
            TrendingChallengesResponse(
                calculatedAt = "2026-09-01T00:00:00Z",
                items = listOf(trending(rank = null), trending(rank = null)),
            ).toDomain()

        assertEquals(listOf(1, 2), snapshot.items.map { it.rank })
    }

    @Test
    fun `순위를 주면 그 값을 그대로 쓴다`() {
        val snapshot = TrendingChallengesResponse(items = listOf(trending(rank = 7))).toDomain()

        assertEquals(7, snapshot.items.single().rank)
    }

    @Test
    fun `입장 가능 여부를 모르면 잠긴 것으로 본다`() {
        val snapshot = TrendingChallengesResponse(items = listOf(trending(joinable = null))).toDomain()

        assertFalse(snapshot.items.single().joinable)
    }

    @Test
    fun `모르는 인증 방식은 수동으로 접는다`() {
        // 자동으로 접으면 앱이 수집하지 않는 신호를 기다리다 매일 실패가 쌓인다.
        val snapshot =
            TrendingChallengesResponse(items = listOf(trending(verificationType = "TELEPATHY"))).toDomain()

        assertEquals(VerificationType.MANUAL, snapshot.items.single().verificationType)
    }

    @Test
    fun `식별자가 없으면 조용히 넘기지 않고 실패로 알린다`() {
        assertFailsWith<ApiException> {
            TrendingChallengesResponse(items = listOf(trending(challengeId = null))).toDomain()
        }
    }

    @Test
    fun `목록이 통째로 없으면 빈 목록으로 다룬다`() {
        val snapshot = TrendingChallengesResponse(items = null).toDomain()

        assertEquals(emptyList(), snapshot.items)
    }

    @Test
    fun `다음 페이지 여부를 안 주면 커서 유무로 판단한다`() {
        val page = ExploreChallengesResponse(items = emptyList(), nextCursor = "c1", hasNext = null).toDomain()

        assertTrue(page.hasNext)
        assertEquals("c1", page.nextCursor)
    }

    @Test
    fun `끝났다고 하면서 커서를 남긴 응답은 커서를 버린다`() {
        // 그대로 두면 다음 페이지를 영원히 요청한다.
        val page = ExploreChallengesResponse(items = emptyList(), nextCursor = "c1", hasNext = false).toDomain()

        assertFalse(page.hasNext)
        assertNull(page.nextCursor)
    }

    private fun trending(
        challengeId: String? = "ch1",
        rank: Int? = 1,
        joinable: Boolean? = true,
        verificationType: String? = "MANUAL",
    ) = TrendingChallengeResponse(
        rank = rank,
        challengeId = challengeId,
        title = "아침 러닝",
        imageUrl = null,
        category = "EXERCISE",
        participantCount = 3,
        recentJoins24h = 2,
        verificationType = verificationType,
        minTier = null,
        joinable = joinable,
        endDate = null,
    )
}
