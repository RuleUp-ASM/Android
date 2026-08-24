package com.ruleup.profile.data.dto

import com.ruleup.domain.entity.category.Category
import com.ruleup.profile.domain.entity.ActivityCalendar
import com.ruleup.profile.domain.entity.CalendarDay
import com.ruleup.profile.domain.entity.CalendarDayDetail
import com.ruleup.profile.domain.entity.CalendarDayItem
import com.ruleup.profile.domain.entity.CalendarDayStatus
import com.ruleup.profile.domain.entity.DayItemAppeal
import com.ruleup.profile.domain.entity.DayItemStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- 활동 캘린더 월 조회 (GET /me/calendar?month=YYYY-MM) ----------
@Serializable
data class CalendarDayResponse(
    @SerialName("date")
    val date: String? = null,
    // ALL_DONE / PARTIAL / FAILED / CHECKING / IN_PROGRESS
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
    // IN_PROGRESS / CHECKING / DONE / FAILED
    @SerialName("status")
    val status: String? = null,
    // 이의 신청 대상 인증 건 ID
    @SerialName("verificationId")
    val verificationId: String? = null,
    // AUTO / MANUAL
    @SerialName("verifiedVia")
    val verifiedVia: String? = null,
    @SerialName("confirmedAt")
    val confirmedAt: String? = null,
    @SerialName("failureReason")
    val failureReason: String? = null,
    // FAILED 일 때만 내려온다
    @SerialName("appeal")
    val appeal: DayItemAppealResponse? = null,
)

/**
 * 명세에는 `remainingThisMonth`·`ineligibleReason` 도 있지만 읽지 않는다 — 이의 횟수 한도가
 * 폐기돼(챌린지 정책 §7.2) 잔여 횟수 개념이 없고, 사유는 `eligible`·`eligibleUntil` 로 이미 드러난다.
 */
@Serializable
data class DayItemAppealResponse(
    @SerialName("eligible")
    val eligible: Boolean? = null,
    @SerialName("eligibleUntil")
    val eligibleUntil: String? = null,
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
                    category = item.category?.let(Category::fromValue),
                    status = DayItemStatus.fromValue(item.status),
                    verificationId = item.verificationId,
                    verifiedVia = item.verifiedVia,
                    confirmedAt = item.confirmedAt,
                    failureReason = item.failureReason,
                    appeal =
                        item.appeal?.let {
                            DayItemAppeal(
                                // 모르면 못 내는 쪽으로 접는다 — 열어 두면 눌렀다 409 를 본다.
                                eligible = it.eligible ?: false,
                                eligibleUntil = it.eligibleUntil,
                            )
                        },
                )
            },
    )
