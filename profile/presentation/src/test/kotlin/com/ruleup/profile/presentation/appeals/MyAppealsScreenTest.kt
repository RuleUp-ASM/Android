package com.ruleup.profile.presentation.appeals

import com.ruleup.verification.domain.entity.AppealHistoryItem
import com.ruleup.verification.domain.entity.AppealTrack
import kotlin.test.Test
import kotlin.test.assertEquals

class MyAppealsScreenTest {
    @Test
    fun `이력 제목은 루틴명과 신청일을 잇는다`() {
        assertEquals("아침 6:30 기상 · 8.2", item(routineTitle = "아침 6:30 기상", date = "2026-08-02").rowTitle())
    }

    @Test
    fun `빈 값은 구분점만 남기지 않는다`() {
        // 루틴명이 비면 "· 8.2" 처럼 앞이 잘린 제목이 나온다.
        assertEquals("8.2", item(routineTitle = "", date = "2026-08-02").rowTitle())
        assertEquals("아침 6:30 기상", item(routineTitle = "아침 6:30 기상", date = "").rowTitle())
    }

    @Test
    fun `신청일은 연도를 떼고 월 일만 남긴다`() {
        assertEquals("8.2", appealDateLabel("2026-08-02"))
        assertEquals("12.25", appealDateLabel("2026-12-25T09:00:00+09:00"))
    }

    @Test
    fun `날짜 형식을 모르면 원문을 그대로 둔다`() {
        // 지어낸 날짜를 보여주는 것보다 원문이 낫다.
        assertEquals("nonsense", appealDateLabel("nonsense"))
    }

    private fun item(
        routineTitle: String,
        date: String,
    ): AppealHistoryItem =
        AppealHistoryItem(
            appealId = "ap_1",
            date = date,
            challengeId = "c_1",
            routineTitle = routineTitle,
            reason = "지하철에서 GPS가 끊겨 체류 기록이 빠졌어요",
            track = AppealTrack.B,
        )
}
