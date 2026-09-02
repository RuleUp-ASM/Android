package com.ruleup.challenge.data.dto

import com.ruleup.challenge.domain.entity.MemberRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 방 상세 응답 매핑. 이 화면은 사용자가 **자기 방의 상태를 판단하는 곳**이라, 모르는 값을
 * 임의로 접으면 없던 사실을 만들어 낸다.
 */
class RoomMappingTest {
    @Test
    fun `표본이 없는 성공률을 0퍼센트로 접지 않는다`() {
        // 갓 만든 방이 실패한 방처럼 보인다 — 화면이 "-" 로 그릴 수 있게 null 을 지킨다.
        val room = RoomResponse(summary = RoomSummaryResponse(roomSuccessRate = null)).toDomain()

        assertNull(room.summary.roomSuccessRate)
    }

    @Test
    fun `모르는 역할은 일반 멤버로 본다`() {
        // 서버가 역할을 늘려도 방이 안 열리면 안 된다. 방장 권한을 주는 쪽으로 접지도 않는다.
        val room = RoomResponse(myRole = "CO_LEADER_V2").toDomain()

        assertEquals(MemberRole.MEMBER, room.myRole)
    }

    @Test
    fun `오늘 인증 상태를 모르면 성공도 실패도 아닌 것으로 둔다`() {
        // 어느 쪽으로 접어도 거짓이 된다 — 실패로 접으면 하지도 않은 실패를 보여 준다.
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
        // 순위·성공률 없는 카드는 랭킹으로 쓸 수 없다 — 0 으로 채우면 꼴찌가 하나 생긴다.
        val room =
            RoomResponse(
                topRanking = listOf(RoomTopRankerResponse(rank = null, successRate = 0.9, userId = "u1")),
            ).toDomain()

        assertTrue(room.topRanking.isEmpty())
    }
}
