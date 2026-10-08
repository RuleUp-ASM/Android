package com.ruleup.challenge.data.dto

import com.ruleup.challenge.domain.entity.MemberRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 방 상세 응답 매핑. */
class RoomMappingTest {
    @Test
    fun `표본이 없는 성공률을 0퍼센트로 접지 않는다`() {
        val room = RoomResponse(summary = RoomSummaryResponse(roomSuccessRate = null)).toDomain()

        assertNull(room.summary.roomSuccessRate)
    }

    @Test
    fun `모르는 역할은 일반 멤버로 본다`() {
        val room = RoomResponse(myRole = "CO_LEADER_V2").toDomain()

        assertEquals(MemberRole.MEMBER, room.myRole)
    }

    @Test
    fun `오늘 인증 상태를 모르면 성공도 실패도 아닌 것으로 둔다`() {
        // 어느 쪽으로 접어도 거짓이 된다
        val room = RoomResponse(myTodayStatus = "SOMETHING_NEW").toDomain()

        assertNull(room.myTodayStatus)
    }

    @Test
    fun `요약이 통째로 없어도 방을 못 열게 하지 않는다`() {
        val room = RoomResponse(summary = null).toDomain()

        assertEquals("", room.summary.title)
        assertEquals(0, room.summary.participantCount)
    }

    @Test
    fun `순위에 필요한 값이 빠진 항목은 버린다`() {
        val room =
            RoomResponse(
                topRanking = listOf(RoomTopRankerResponse(rank = null, successRate = 0.9, userId = "u1")),
            ).toDomain()

        assertTrue(room.topRanking.isEmpty())
    }

    @Test
    fun `루틴 진행률은 퍼센트를 0에서 1 사이 비율로 옮긴다`() {
        val progress =
            RoomResponse(routineProgress = RoomRoutineProgressResponse(myProgressRate = 72.5, mySuccessDays = 18, myTargetDays = 25))
                .toDomain()
                .routineProgress!!

        assertEquals(0.725, progress.myProgressRate, 1e-9)
        assertEquals(18, progress.mySuccessDays)
        assertEquals(25, progress.myTargetDays)
    }

    @Test
    fun `목표일이 아직 계산 전이면 분모가 없는 것으로 본다`() {
        val progress = RoomResponse(routineProgress = RoomRoutineProgressResponse(myTargetDays = 0)).toDomain().routineProgress!!

        assertFalse(progress.hasTargetDays)
    }

    @Test
    fun `루틴 진행률이 없으면 비워 둔다`() {
        assertNull(RoomResponse().toDomain().routineProgress)
    }
}
