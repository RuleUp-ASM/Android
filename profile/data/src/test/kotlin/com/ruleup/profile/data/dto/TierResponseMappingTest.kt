package com.ruleup.profile.data.dto

import com.ruleup.domain.entity.user.Tier
import com.ruleup.profile.domain.entity.ScoreChangeReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 티어 응답 매핑. 승·강등은 **서버 판정**이고 화면은 그걸 옮겨 적을 뿐이라, 여기서 값을 지어내면
 * 사용자는 실제로는 오지 않은 강등을 보거나 이미 온 강등을 못 본다.
 */
class MyTierResponseMappingTest {
    @Test
    fun `표시 티어를 안 주면 실제 티어로 떨어뜨린다`() {
        // 없는 유예를 있는 것처럼 그리면 방 입장 판정과 화면이 어긋난다.
        val tier = MyTierResponse(tier = "GOLD", displayTier = null).toDomain()

        assertEquals(Tier.GOLD, tier.displayTier)
    }

    @Test
    fun `최상위 티어면 다음 티어까지의 거리를 만들지 않는다`() {
        val tier = MyTierResponse(tier = "RUBY", score = 1500, promotion = null).toDomain()

        assertNull(tier.promotion)
    }

    @Test
    fun `강등 경계 중 하나라도 없으면 강등 안내를 만들지 않는다`() {
        // 한쪽만 아는 채로 "몇 점에 떨어진다"고 말하면 그 숫자가 곧 거짓말이 된다.
        val tier =
            MyTierResponse(
                tier = "SILVER",
                demotion = TierDemotionResponse(graceFloor = 80, demoteAt = null),
            ).toDomain()

        assertNull(tier.demotion)
    }

    @Test
    fun `모르는 변동 사유는 사유만 비우고 증감폭은 남긴다`() {
        // 사유 enum 이 늘었다고 그 행이 통째로 사라지면 사용자는 점수가 왜 줄었는지 알 수 없다.
        val tier =
            MyTierResponse(
                recentChanges = listOf(ScoreChangeResponse(date = "2026-08-01", reason = "SEASON_RESET", delta = -3)),
            ).toDomain()

        assertNull(tier.recentChanges.single().reason)
        assertEquals(-3, tier.recentChanges.single().delta)
    }

    @Test
    fun `날짜 없는 변동은 목록에 세우지 않는다`() {
        // 언제 일어난 변동인지 모르면 "최근"이라는 목록에 놓을 자리가 없다.
        val tier = MyTierResponse(recentChanges = listOf(ScoreChangeResponse(date = null, delta = 5))).toDomain()

        assertTrue(tier.recentChanges.isEmpty())
    }

    @Test
    fun `받은 티어 상세는 그대로 전한다`() {
        val tier =
            MyTierResponse(
                tier = "GOLD",
                score = 370,
                displayTier = "GOLD",
                graceBand = false,
                promotion = TierPromotionResponse(nextTier = "DIAMOND", pointsToPromote = 130),
                demotion = TierDemotionResponse(graceFloor = 280, demoteAt = 279),
                recentChanges =
                    listOf(ScoreChangeResponse(date = "2026-07-21", reason = "CYCLE_SUCCESS", challengeId = "c_301", delta = 5)),
            ).toDomain()

        assertEquals(130, tier.promotion?.pointsToPromote)
        assertEquals(279, tier.demotion?.demoteAt)
        assertEquals(ScoreChangeReason.CYCLE_SUCCESS, tier.recentChanges.single().reason)
    }
}

/**
 * 티어 히스토리 매핑. 그래프의 x 축이라 **월이 없는 점은 세울 자리가 없고**, 표본이 없는 계정에
 * 역대 최고를 지어내면 없던 기록이 생긴다.
 */
class TierHistoryResponseMappingTest {
    @Test
    fun `역대 최고에 날짜가 없으면 최고 기록으로 세우지 않는다`() {
        val history = TierHistoryResponse(best = TierBestResponse(tier = "GOLD", score = 85, date = null)).toDomain()

        assertNull(history.best)
    }

    @Test
    fun `월이 없는 스냅샷은 그래프에 올리지 않는다`() {
        val history =
            TierHistoryResponse(
                monthly = listOf(TierSnapshotResponse(month = null, endTier = "GOLD", endScore = 300)),
            ).toDomain()

        assertTrue(history.monthly.isEmpty())
    }

    @Test
    fun `받은 월말 스냅샷은 순서 그대로 전한다`() {
        val history =
            TierHistoryResponse(
                monthly =
                    listOf(
                        TierSnapshotResponse(month = "2026-05", endTier = "SILVER", endScore = 188),
                        TierSnapshotResponse(month = "2026-06", endTier = "GOLD", endScore = 302),
                    ),
                retentionNote = "1년 보관",
            ).toDomain()

        assertEquals(listOf("2026-05", "2026-06"), history.monthly.map { it.month })
        assertEquals(Tier.GOLD, history.monthly.last().endTier)
        assertEquals("1년 보관", history.retentionNote)
    }
}
