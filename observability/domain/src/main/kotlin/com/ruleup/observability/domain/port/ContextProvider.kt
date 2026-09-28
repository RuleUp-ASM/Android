package com.ruleup.observability.domain.port

import com.ruleup.observability.domain.model.ObsContext

/** 이벤트 발생 시점의 동적 컨텍스트를 제공한다. */
fun interface ContextProvider {
    fun current(): ObsContext
}
