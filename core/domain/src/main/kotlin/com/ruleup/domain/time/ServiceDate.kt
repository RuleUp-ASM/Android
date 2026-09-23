package com.ruleup.domain.time

import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * 서비스 기준 날짜.
 *
 * 판정·마감·귀속일이 전부 **서버 KST 하루 단위**다. 화면이 기기 로컬 날짜로 "오늘"을 정하면 해외
 * 체류 중에 홈은 16일, 방은 17일 건을 오늘이라 가리키고 "이의 오늘까지"가 "내일까지"로 바뀐다.
 * 같은 순간을 두 날짜로 말하는 셈이라, 표시 기준을 서버와 같은 곳에 맞춘다.
 *
 * **사용자가 직접 고르는 날짜(챌린지 시작일 선택 등)에는 쓰지 않는다** — 그건 판정이 아니라 입력이고,
 * 달력이 기기 기준으로 열리는 편이 자연스럽다.
 */
object ServiceDate {
    val ZONE: ZoneId = ZoneId.of("Asia/Seoul")

    fun today(): LocalDate = LocalDate.now(ZONE)

    /**
     * 서버가 준 ISO-8601 시각을 서비스 기준(KST)으로 옮긴다. 오프셋이 없으면 null.
     *
     * **오프셋을 무시하고 문자열을 자르면 안 된다** — 서버가 `Z`(UTC)로 내려주는 응답이 섞여 있어,
     * 자르면 10:17 이 01:17 로 보이고 자정 근처에서는 날짜까지 하루 어긋난다(ROOM-01).
     */
    fun atZone(iso: String): ZonedDateTime? = runCatching { OffsetDateTime.parse(iso).atZoneSameInstant(ZONE) }.getOrNull()
}
