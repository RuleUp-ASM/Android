package com.ruleup.domain.time

import java.time.LocalDate
import java.time.ZoneId

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
    private val ZONE: ZoneId = ZoneId.of("Asia/Seoul")

    fun today(): LocalDate = LocalDate.now(ZONE)
}
