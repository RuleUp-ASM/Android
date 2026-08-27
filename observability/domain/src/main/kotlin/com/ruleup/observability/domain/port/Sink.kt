package com.ruleup.observability.domain.port

import com.ruleup.observability.domain.event.ObsEvent

/**
 * 이벤트 출구. 도메인이 아는 출구는 **하나뿐**이고, 팬아웃·채널 라우팅·실패 격리·실패 집계는
 * 전부 data 쪽 합성 구현의 몫이다.
 *
 * **[emit] 은 절대 예외를 던지지 않는다** — 도메인이 감싸지 않으므로 이 계약이 곧 *"로깅은 앱을
 * 죽이지 않는다"* 를 지탱한다. 블로킹하지 않으며 임의 스레드에서 호출될 수 있다.
 */
interface Sink {
    fun emit(event: ObsEvent)

    /**
     * 버퍼에 쌓인 이벤트를 즉시 내보낸다. 버퍼가 없는 구현은 기본 no-op 그대로 두고, **비동기 버퍼를
     * 두는 구현은 반드시 재정의한다** — 크래시 직전 이벤트가 관측 데이터 중 가장 값지다.
     *
     * 프로세스 종료 직전(`onTrimMemory`·크래시 핸들러·백그라운드 전환)에 불리므로 [emit] 과 달리
     * **블로킹이 허용된다.** 다만 ANR 을 막을 자체 타임아웃을 두고, 예외는 던지지 않는다.
     */
    fun flush() {}
}
