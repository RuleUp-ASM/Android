package com.ruleup.analytics

/**
 * 비즈니스 이벤트 로깅 추상화.
 *
 * 비즈니스 코드(UseCase/ViewModel)는 Firebase 등 구체 SDK 를 모른 채 이 인터페이스에만 의존한다.
 * 플랫폼별 구현(Android = Firebase Analytics, iOS = 스텁)이 Metro 그래프에 [AnalyticsLogger] 로 바인딩된다.
 */
interface AnalyticsLogger {
    /** [event] 를 분석 백엔드로 전송한다. */
    fun log(event: AnalyticsEvent)

    /** 로그인/로그아웃 시 사용자 식별자를 설정한다. null 이면 해제. PII(이메일·이름 등)는 넣지 않는다. */
    fun setUserId(id: String?)

    /** 세그먼트 분석용 사용자 속성을 설정한다. PII 금지. */
    fun setUserProperty(
        key: String,
        value: String,
    )
}
