package com.ruleup.observability.domain.port

import com.ruleup.observability.domain.event.Channel
import com.ruleup.observability.domain.model.Severity

/** 이벤트 수집 여부를 결정하는 게이트. */
fun interface Policy {
    /** 핫패스 게이트. */
    fun isEnabled(
        channel: Channel,
        severity: Severity,
        tag: String?,
    ): Boolean
}
