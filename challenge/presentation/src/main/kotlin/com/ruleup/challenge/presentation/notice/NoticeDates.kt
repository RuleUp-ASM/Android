package com.ruleup.challenge.presentation.notice

/**
 * 공지 시각 표시용 날짜 라벨. 서버는 ISO-8601(+09:00 오프셋)을 내려주며,
 * 공지에는 날짜 단위 표기면 충분하다 (예: "2026.07.17"). 파싱 불가 문자열은 그대로 노출한다.
 */
internal fun noticeDateLabel(isoDateTime: String): String {
    val date = isoDateTime.substringBefore('T')
    val parts = date.split('-')
    if (parts.size != 3) return isoDateTime
    return parts.joinToString(".")
}
