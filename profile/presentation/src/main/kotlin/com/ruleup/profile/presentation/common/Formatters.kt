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
