package com.ruleup.observability.domain.port

import com.ruleup.observability.domain.event.Channel
import com.ruleup.observability.domain.model.Severity

/**
 * 이벤트 수집 여부를 결정하는 게이트. **도메인이 아는 판단은 이것 하나뿐이다** — [isEnabled] 만이
 * 페이로드 생성 전에 호출되어야 해서 파사드에 인라이닝될 위치가 필요하다.
 *
 * 중복 제거·레이트리밋처럼 이벤트가 완성된 뒤의 판단은 전부 [Sink] 구현 안에 있다.
 */
fun interface Policy {
    /**
     * 핫패스 게이트. 락·순회·IO 없이 필드 읽기와 비교로 끝나야 한다 — 여기서 비싸지면
     * "할당 전에 끊기"라는 존재 이유가 사라진다. 던지지 않고, 판단이 안 서면 true(관대하게).
     *
     * **불변 설정 스냅샷 하나를 지역변수로 한 번만 읽고 그 안에서만 판단한다** — 여러 번 읽으면
     * 존재한 적 없는 조합으로 판정하고 인스펙터가 보여주는 설정과도 갈라진다.
     *
     * **floor 는 채널별로 독립이어야 한다.** 진단 floor 를 다른 채널에 적용하면 그 페이로드는 전부
     * [Severity.INFO] 라 `DIAGNOSTIC = WARN` 하나로 분석·성능 지표가 통째로 사라진다. 설정에 없는
     * 채널은 제한 없음으로 본다 — 새 채널이 조용히 죽는 것보다 새는 편이 낫다.
     *
     * @param severity 페이로드의 심각도. 진단 외 채널은 [Severity.INFO] 로 간주된다.
     * @param tag 진단 채널의 태그. 진단 외 채널이면 null 이며, null 은 태그 단위로 판단할 근거가
     *   없다는 뜻이라 채널·심각도 수준에서만 판단하고 통과시킨다.
     */
    fun isEnabled(
        channel: Channel,
        severity: Severity,
        tag: String?,
    ): Boolean
}
