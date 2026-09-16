package com.ruleup.observability.domain.event

/**
 * 관측 채널. 사용자 행동은 여기 없다 — 비즈니스 이벤트는 수명주기와 전송 보장이 달라
 * `:logging` 이 따로 맡는다(`BizLogger`).
 */
enum class Channel { DIAGNOSTIC, PERFORMANCE }
