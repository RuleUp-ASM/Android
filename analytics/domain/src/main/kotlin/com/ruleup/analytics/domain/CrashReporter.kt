package com.ruleup.analytics.domain

/**
 * 처리된(치명적이지 않은) 예외를 크래시 리포터에 non-fatal 로 기록하는 추상화.
 *
 * 비즈니스 코드는 Firebase 등 구체 SDK 를 모른 채 이 포트에만 의존한다. 구현(Crashlytics)은 Hilt 그래프에
 * 바인딩된다. uncaught 크래시·ANR 은 SDK 가 자동 수집하므로, 여기서는 try/catch 로 삼킨 실패를 보강한다.
 * [keys]·예외 메시지에 좌표·헬스 원값·토큰 등 PII 는 절대 담지 않는다(비식별 진단 컨텍스트만).
 */
interface CrashReporter {
    fun recordException(
        throwable: Throwable,
        keys: Map<String, String> = emptyMap(),
    )
}
