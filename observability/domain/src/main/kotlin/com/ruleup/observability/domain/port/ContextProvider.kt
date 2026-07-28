package com.ruleup.observability.domain.port

import com.ruleup.observability.domain.model.ObsContext

/**
 * 이벤트 발생 시점의 동적 컨텍스트를 제공한다.
 *
 * 읽기 전용이다. 갱신 주체가 네비게이션 콜백 하나뿐이고 그건 `:app` 에 있어
 * `:observability:data` 를 직접 볼 수 있으므로, 쓰기 계약을 도메인에 둘 이유가 없다.
 * 구현체가 가변 홀더를 노출하고 `:app` 이 배선한다.
 *
 * 계약:
 * - [current] 는 이벤트마다 호출된다. 필드 읽기 수준으로 저렴해야 한다.
 *   디스크·DataStore·네트워크 접근 금지.
 * - 임의 스레드에서 호출될 수 있다. 구현은 스레드 안전해야 한다(@Volatile 홀더 권장).
 * - **필드를 하나씩 갱신하지 말고 불변 [ObsContext] 를 통째로 교체한다.**
 *   [current] 는 항상 실제로 존재했던 조합을 반환해야 한다.
 * - 던지지 않는다. 값을 모르면 null 필드로 반환한다.
 */
fun interface ContextProvider {
    fun current(): ObsContext
}
