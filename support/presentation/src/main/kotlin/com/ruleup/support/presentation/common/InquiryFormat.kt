package com.ruleup.support.presentation.common

import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/** 화면에 보이는 접수번호. */
fun shortInquiryId(inquiryId: String): String =
    if (inquiryId.length <= SHORT_ID_LENGTH) inquiryId else inquiryId.take(SHORT_ID_LENGTH) + "…"

private const val SHORT_ID_LENGTH = 18

/** "2026-09-05T14:22:00Z" → "09.05"(기기 시간대). */
fun shortDate(
    iso: String,
    zone: ZoneId = ZoneId.systemDefault(),
): String = local(iso, zone)?.format(SHORT_DATE) ?: iso

/** "2026-09-05T14:22:00Z" → "09.05 23:22"(KST). */
fun shortDateTime(
    iso: String,
    zone: ZoneId = ZoneId.systemDefault(),
): String = local(iso, zone)?.format(SHORT_DATE_TIME) ?: iso

private fun local(
    iso: String,
    zone: ZoneId,
): ZonedDateTime? = runCatching { OffsetDateTime.parse(iso).atZoneSameInstant(zone) }.getOrNull()

private val SHORT_DATE = DateTimeFormatter.ofPattern("MM.dd")
private val SHORT_DATE_TIME = DateTimeFormatter.ofPattern("MM.dd HH:mm")
