package com.ruleup.challenge.presentation.detail.viewmodel

import java.time.LocalDate

/** 판정 시작일(`countFromCycle`, yyyy-MM-dd) → "9월 21일". 형식이 다르면 받은 값을 그대로 보여 준다. */
internal fun cycleStartLabel(countFromCycle: String): String =
    runCatching { LocalDate.parse(countFromCycle) }
        .map { "${it.monthValue}월 ${it.dayOfMonth}일" }
        .getOrDefault(countFromCycle)
