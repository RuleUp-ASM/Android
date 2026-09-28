package com.ruleup.challenge.presentation.common

import com.ruleup.challenge.domain.entity.ParamSpec

/** 목표값 단위. */
internal val ParamSpec.unitLabel: String?
    get() =
        when (unit?.lowercase()) {
            "min", "minute", "minutes" -> "분"
            "step", "steps" -> "걸음"
            "hour", "hours" -> "시간"
            "second", "seconds", "sec" -> "초"
            else -> unit
        }
