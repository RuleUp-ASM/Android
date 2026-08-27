package com.ruleup.observability.domain.event

import com.ruleup.observability.domain.model.Attributes
import com.ruleup.observability.domain.model.Severity

sealed interface ObsPayload {
    val channel: Channel

    val attrs: Attributes

    /**
     * 게이트 판정 축. 진단 외 채널은 [Severity.INFO] 다 — floor 가 **채널별로 독립**이라
     * 이 기본값이 진단 채널 floor 에 걸려 죽지 않는다([com.ruleup.observability.domain.port.Policy.isEnabled]).
     */
    val severity: Severity get() = Severity.INFO

    /** 태그별 floor 오버라이드 판정용. 진단 외 채널은 null. */
    val tag: String? get() = null
}
