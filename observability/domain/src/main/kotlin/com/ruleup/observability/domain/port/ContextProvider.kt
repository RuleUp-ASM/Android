package com.ruleup.observability.domain.port

import com.ruleup.observability.domain.model.ObsContext

/**
 * 이벤트 발생 시점의 동적 컨텍스트를 제공한다. 읽기 전용이며, 이벤트마다 호출되므로 필드 읽기
 * 수준으로 저렴해야 한다(디스크·네트워크 금지). 임의 스레드에서 불리고 던지지 않는다.
 *
 * **필드를 하나씩 갱신하지 말고 불변 [ObsContext] 를 통째로 교체한다** — [current] 는 항상
 * 실제로 존재했던 조합을 반환해야 한다.
 */
fun interface ContextProvider {
    fun current(): ObsContext
}
