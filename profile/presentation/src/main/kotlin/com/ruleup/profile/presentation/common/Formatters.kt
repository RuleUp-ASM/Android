package com.ruleup.profile.presentation.common

import java.util.Locale

/** 78.0 → "78", 78.4 → "78.4" — 온도·퍼센트 공용 표기. */
internal fun Double.trimLabel(): String =
    if (this % 1.0 == 0.0) {
        toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", this)
    }

/** ISO 날짜/시각("2026-05-10" 또는 "…T…")을 "2026.05.10" 표기로. 파싱 불가 문자열은 그대로. */
internal fun dateDotLabel(iso: String): String {
    val date = iso.substringBefore('T')
    val parts = date.split('-')
    if (parts.size != 3) return iso
    return parts.joinToString(".")
}

/**
 * 제재 해제 시각("2026-10-15T00:00:00+09:00" → "2026. 10. 15 00:00").
 *
 * 날짜만 보이면 그날 언제 풀리는지 몰라 사용자가 하루를 통째로 기다린다. 파싱 불가 문자열은 그대로 둔다.
 */
internal fun sanctionUntilLabel(iso: String): String {
    val parts = iso.substringBefore('T').split('-')
    if (parts.size != 3) return iso
    val date = parts.joinToString(". ")
    val time = iso.substringAfter('T', "").take(5)
    return if (time.length == 5) "$date $time" else date
}
