package com.ruleup.observability.domain.model

/**
 * 이벤트 발생 시점의 동적 컨텍스트. 유저 식별자·세션은 담지 않는다 — 분석 SDK 가 자기 상태로
 * 관리하므로 `:app` 이 `UserIdentitySync` 로 직접 알린다.
 *
 * [currentScreen] 만 SDK 가 대신해줄 수 없다. `ResourceProbe`·`DiagnosticPayload` 에는 화면 필드가
 * 없어서 **"어느 화면에서 난 잼/에러인지"를 오직 여기서만 알 수 있다.**
 */
data class ObsContext(
    val currentScreen: ScreenKey?,
)
