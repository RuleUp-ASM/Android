package com.ruleup.observability.domain.event

import com.ruleup.observability.domain.model.Attributes
import com.ruleup.observability.domain.model.ErrorInfo
import com.ruleup.observability.domain.model.Severity

data class DiagnosticPayload(
    override val severity: Severity,
    override val tag: String,
    val message: String,
    /** 예외의 값 표현. `Throwable` 을 직접 담지 않는 이유는 [ErrorInfo] 문서 참고. */
    val cause: ErrorInfo? = null,
    override val attrs: Attributes = Attributes.EMPTY,
) : ObsPayload {
    override val channel: Channel get() = Channel.DIAGNOSTIC
}
