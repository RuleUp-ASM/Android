package com.ruleup.observability.domain.model

/** 자원 스냅샷을 뜬 계기. */
enum class ProbeTrigger { JANK_DETECTED, TTI_SLOW, PERIODIC }
