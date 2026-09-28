package com.ruleup.challenge.data.dto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 랭킹 응답 매핑. */
class RankingMappingTest {
    @Test
    fun `미등재 순위를 0 으로 접지 않는다`() {
        val ranking = RankingResponse(me = MyRankResponse(rank = null, successRate = null)).toDomain()

        assertNull(ranking.me.rank)
        assertNull(ranking.me.successRate)
    }

    @Test
    fun `등재 여부를 안 주면 순위 유무로 판정한다`() {
        assertTrue(RankingResponse(me = MyRankResponse(rank = 3, ranked = null)).toDomain().me.ranked)
        assertFalse(RankingResponse(me = MyRankResponse(rank = null, ranked = null)).toDomain().me.ranked)
    }

    @Test
    fun `등재 여부를 주면 그 값을 그대로 쓴다`() {
        val ranking = RankingResponse(me = MyRankResponse(rank = 3, ranked = false)).toDomain()

        assertFalse(ranking.me.ranked)
    }

    @Test
    fun `내 순위 정보가 통째로 없어도 화면을 못 그리게 하지 않는다`() {
        // 남의 순위는 보여 줄 수 있다
        val ranking = RankingResponse(me = null, items = emptyList()).toDomain()

        assertNull(ranking.me.rank)
        assertEquals(0, ranking.me.participations)
    }

    @Test
    fun `참여 횟수는 없으면 0 으로 본다`() {
        // 등재 기준(10회)을 세는 값이라, 없으면 아직 안 한 것이 맞다.
        val ranking = RankingResponse(me = MyRankResponse(participations = null)).toDomain()

        assertEquals(0, ranking.me.participations)
    }

    @Test
    fun `목록이 통째로 없으면 빈 목록으로 다룬다`() {
        assertEquals(emptyList(), RankingResponse(items = null).toDomain().items)
    }
}
