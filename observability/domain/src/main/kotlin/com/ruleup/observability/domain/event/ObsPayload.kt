package com.ruleup.observability.domain.event

import com.ruleup.observability.domain.model.Attributes
import com.ruleup.observability.domain.model.Severity

sealed interface ObsPayload {
    val channel: Channel

    val attrs: Attributes

    /** 게이트 판정 축. */
    val severity: Severity get() = Severity.INFO

    /** 태그별 floor 오버라이드 판정용. */
    val tag: String? get() = null
}
