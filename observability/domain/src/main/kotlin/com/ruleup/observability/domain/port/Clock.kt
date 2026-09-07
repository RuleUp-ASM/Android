package com.ruleup.observability.domain.port

/**
 * 이벤트 타임스탬프 발급기. 이벤트마다 두 함수가 한 번씩 호출되므로 저렴해야 하고,
 * 스레드 안전해야 하며 예외를 던지지 않는다.
 */
interface Clock {
    /** 벽시계(Unix epoch, ms). NTP 동기화·시간 변경으로 뒤로 점프하므로 **순서 판단에 쓰면 안 된다.** */
    fun epochMillis(): Long

    /**
     * 단조 증가 시계(ns). 이벤트 순서와 경과 시간 계산용이며, 기준점이 프로세스마다 리셋되므로
     * **프로세스 간 비교는 불가**하다(오프라인 버퍼를 넘나드는 정렬에는 쓸 수 없다).
     */
    fun monotonicNanos(): Long
}
