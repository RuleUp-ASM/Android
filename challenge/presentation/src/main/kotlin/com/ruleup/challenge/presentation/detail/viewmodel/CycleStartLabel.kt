package com.ruleup.challenge.presentation.detail.viewmodel

import java.time.LocalDate

/** 판정 시작일(`countFromCycle`, yyyy-MM-dd) → "9월 21일". */
internal fun cycleStartLabel(countFromCycle: String): String =
    runCatching { LocalDate.parse(countFromCycle) }
        .map { "${it.monthValue}월 ${it.dayOfMonth}일" }
        .getOrDefault(countFromCycle)
