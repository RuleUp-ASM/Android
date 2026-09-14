package com.ruleup.observability.domain.model

/**
 * 자원 스냅샷을 뜬 계기.
 *
 * [TTI_SLOW] 는 `:tti` 모듈이 느린 화면을 발견했을 때 쓰라고 남겨 둔 값이다 — 지금은 생산자가
 * 없다. 값을 지우면 나중에 붙일 때 이름을 다시 정해야 하고, 그 사이 쌓인 기록과도 어긋난다.
 */
enum class ProbeTrigger { JANK_DETECTED, TTI_SLOW, PERIODIC }
