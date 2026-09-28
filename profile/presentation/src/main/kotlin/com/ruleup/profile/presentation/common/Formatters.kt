package com.ruleup.profile.presentation.common

import java.util.Locale

/** 78.0 → "78", 78.4 → "78.4" */
internal fun Double.trimLabel(): String =
    if (this % 1.0 == 0.0) {
        toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", this)
    }

/** ISO 날짜/시각("2026-05-10" 또는 "…T…")을 "2026.05.10" 표기로. */
internal fun dateDotLabel(iso: String): String {
    val date = iso.substringBefore('T')
    val parts = date.split('-')
    if (parts.size != 3) return iso
    return parts.joinToString(".")
}

/** 제재 해제 시각("2026-10-15T00:00:00+09:00" → "2026. 10. 15 00:00"). */
internal fun sanctionUntilLabel(iso: String): String =
    com.ruleup.domain.time.ServiceDate
        .atZone(iso)
        ?.format(
            java.time.format.DateTimeFormatter
                .ofPattern("yyyy. MM. dd HH:mm"),
        )
        ?: runCatching {
            java.time.LocalDate
                .parse(
                    iso,
                ).format(
                    java.time.format.DateTimeFormatter
                        .ofPattern("yyyy. MM. dd"),
                )
        }.getOrDefault(iso)
