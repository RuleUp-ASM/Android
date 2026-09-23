package com.ruleup.challenge.presentation.detail.component

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 방 피드의 날짜·시각 표기.
 *
 * 서버 응답에 `Z`(UTC) 표기가 섞여 있어 **문자열을 자르면 9시간이 어긋난다** — 같은 인증이 방
 * 상세에는 10:17, 피드에는 01:17 로 떴고 자정 근처에서는 날짜까지 하루 밀렸다(ROOM-01).
 */
class RoomDatesTest {
    @Test
    fun `UTC 로 온 시각을 KST 로 옮겨 보여준다`() {
        assertEquals("10:17", feedTimeLabel("2026-09-22T01:17:00Z"))
    }

    @Test
    fun `KST 오프셋으로 온 시각은 그대로 보여준다`() {
        assertEquals("10:17", feedTimeLabel("2026-09-22T10:17:00+09:00"))
    }

    @Test
    fun `자정 경계에서 날짜가 하루 밀리지 않는다`() {
        // UTC 22일 16:00 은 KST 23일 01:00 이다. 문자열을 자르면 22일로 읽힌다.
        assertEquals("오늘", feedDateHeader("2026-09-22T16:00:00Z", today = LocalDate.of(2026, 9, 23)))
        assertEquals("2026-09-23", feedDateKey("2026-09-22T16:00:00Z"))
    }

    @Test
    fun `오프셋이 없는 값은 이미 서비스 기준으로 보고 그대로 쓴다`() {
        // 파싱에 실패했다고 빈칸으로 두면 멀쩡한 피드가 시각 없이 그려진다.
        assertEquals("10:17", feedTimeLabel("2026-09-22T10:17:00"))
        assertEquals("2026-09-22", feedDateKey("2026-09-22T10:17:00"))
    }
}
