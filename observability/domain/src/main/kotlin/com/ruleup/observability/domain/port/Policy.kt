package com.ruleup.observability.domain.port

import com.ruleup.observability.domain.event.Channel
import com.ruleup.observability.domain.model.Severity

/**
 * 이벤트 수집 여부를 결정하는 게이트.
 *
 * **도메인이 아는 판단은 이것 하나뿐이다.** 중복 제거·레이트리밋은 상태를 갖는 전송 정책이라
 * [Sink] 구현 안에 있고, 게이트 설정의 원본·병합·조회도 전부 data 쪽 사정이다.
 *
 * 이 포트만 도메인에 남은 이유는 하나다 — [isEnabled] 는 **페이로드가 만들어지기 전에** 호출되어야
 * 하고, 그러려면 호출부에 인라이닝되는 위치, 즉 파사드에서 보여야 한다. 나머지 판단은 이벤트가
 * 완성된 뒤라 어디서든 할 수 있다.
 */
fun interface Policy {
    /**
     * 핫패스 게이트. 호출부의 inline API 가 페이로드를 만들기 전에 호출한다.
     *
     * 계약:
     * - @Volatile 필드 읽기 + 비교 수준으로 끝나야 한다. 락·맵 순회·IO 금지.
     *   이 함수의 존재 이유가 "할당 전에 끊기" 이므로, 여기서 비싸지면 게이트의 의미가 없다.
     *   (단일 해시 조회는 순회가 아니므로 허용된다.)
     * - **불변 설정 스냅샷 하나를 지역변수로 한 번만 읽고 그 안에서만 판단한다.**
     *   여러 번 읽으면 판단 도중 설정이 교체되어 존재한 적 없는 조합으로 판정할 수 있고,
     *   인스펙터가 보여주는 설정과도 갈라진다.
     * - 예외를 던지지 않는다. 판단이 서지 않으면 true(관대하게) 를 반환한다.
     * - 임의 스레드에서 호출될 수 있다.
     * - **floor 는 채널별로 독립이어야 한다.** 진단 채널의 floor 를 비즈니스·성능 채널에 적용하면,
     *   그 페이로드들은 전부 [Severity.INFO] 이므로 `DIAGNOSTIC = WARN` 설정 하나로
     *   **분석·성능 지표가 통째로 사라진다.** 설정에 없는 채널은 제한 없음으로 본다 —
     *   새 채널이 추가됐을 때 조용히 죽는 것보다 새는 편이 낫다.
     *
     * @param severity 페이로드의 심각도. 진단 외 채널은 [Severity.INFO] 로 간주된다.
     * @param tag 진단 채널의 태그. 태그별 오버라이드 판단에 쓴다. 진단 외 채널이면 null.
     *
     * [tag] 가 null 이면 태그 단위로 판단할 근거가 없다는 뜻이므로, 채널·심각도 수준에서만
     * 판단하고 나머지는 통과시킨다(모를수록 관대하게).
     */
    fun isEnabled(
        channel: Channel,
        severity: Severity,
        tag: String?,
    ): Boolean
}
