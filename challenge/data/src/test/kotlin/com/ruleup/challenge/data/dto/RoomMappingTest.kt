package com.ruleup.challenge.data.dto

import com.ruleup.challenge.domain.entity.MemberRole
import kotlin.test.Test
import kotlin.test.assertEquals
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
}
