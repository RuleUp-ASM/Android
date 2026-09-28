package com.ruleup.domain.time

import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** 서비스 기준 날짜. */
object ServiceDate {
    val ZONE: ZoneId = ZoneId.of("Asia/Seoul")

    fun today(): LocalDate = LocalDate.now(ZONE)

    /** 서버가 준 ISO-8601 시각을 서비스 기준(KST)으로 옮긴다. */
    fun atZone(iso: String): ZonedDateTime? = runCatching { OffsetDateTime.parse(iso).atZoneSameInstant(ZONE) }.getOrNull()
}
