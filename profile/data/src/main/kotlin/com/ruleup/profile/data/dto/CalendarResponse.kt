package com.ruleup.profile.data.dto

import com.ruleup.entity.user.InterestCategory
import com.ruleup.profile.domain.entity.ActivityCalendar
import com.ruleup.profile.domain.entity.CalendarDay
import com.ruleup.profile.domain.entity.CalendarDayDetail
import com.ruleup.profile.domain.entity.CalendarDayItem
import com.ruleup.profile.domain.entity.CalendarDayStatus
import com.ruleup.profile.domain.entity.DayItemStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- 활동 캘린더 월 조회 (GET /me/calendar?month=YYYY-MM) ----------
@Serializable
data class CalendarDayResponse(
    @SerialName("date")
    val date: String? = null,
    // ALL_DONE / PARTIAL / FAILED / PENDING / NOT_TARGET
    @SerialName("status")
    val status: String? = null,
    @SerialName("successCount")
    val successCount: Int? = null,
    @SerialName("targetCount")
    val targetCount: Int? = null,
)

@Serializable
data class ActivityCalendarResponse(
    @SerialName("month")
    val month: String? = null,
    // 판정 대상일만 포함 — 없는 날짜는 비대상일로 렌더링
    @SerialName("days")
    val days: List<CalendarDayResponse>? = null,
)

internal fun ActivityCalendarResponse.toDomain(): ActivityCalendar =
    ActivityCalendar(
        month = month.orEmpty(),
        days =
            days.orEmpty().mapNotNull { day ->
                val date = day.date ?: return@mapNotNull null
                CalendarDay(
                    date = date,
                    status = CalendarDayStatus.fromValue(day.status),
                    successCount = day.successCount ?: 0,
                    targetCount = day.targetCount ?: 0,
                )
            },
    )

// ---------- 캘린더 일자 상세 (GET /me/calendar/{date}) ----------
@Serializable
data class CalendarDayItemResponse(
    @SerialName("challengeId")
    val challengeId: String? = null,
    @SerialName("title")
    val title: String? = null,
    // RoutineOutcome 카테고리 스냅샷 (예: WAKE_UP)
    @SerialName("category")
    val category: String? = null,
    // SUCCESS / FAILED / PENDING / NOT_REQUIRED
    @SerialName("status")
    val status: String? = null,
    // AUTO / MANUAL / MANUAL_FALLBACK
    @SerialName("verifiedVia")
    val verifiedVia: String? = null,
    @SerialName("verifiedAt")
    val verifiedAt: String? = null,
    @SerialName("failureReason")
    val failureReason: String? = null,
)

@Serializable
data class CalendarDayDetailResponse(
    @SerialName("date")
    val date: String? = null,
    @SerialName("items")
    val items: List<CalendarDayItemResponse>? = null,
)

internal fun CalendarDayDetailResponse.toDomain(): CalendarDayDetail =
    CalendarDayDetail(
        date = date.orEmpty(),
        items =
            items.orEmpty().mapNotNull { item ->
                val id = item.challengeId ?: return@mapNotNull null
                CalendarDayItem(
                    challengeId = id,
                    title = item.title.orEmpty(),
                    category = item.category?.let(InterestCategory::fromValue),
                    status = DayItemStatus.fromValue(item.status),
                    verifiedVia = item.verifiedVia,
                    verifiedAt = item.verifiedAt,
                    failureReason = item.failureReason,
                )
            },
    )
