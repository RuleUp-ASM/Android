package com.ruleup.observability.domain.model

/**
 * 이벤트 발생 시점의 동적 컨텍스트.
 *
 * 유저 식별자·세션은 여기 담지 않는다. 분석 SDK 는 그것들을 **이벤트 필드가 아니라 자기 상태로**
 * 관리하므로(`setUserId`, 자체 세션 관리), 이벤트마다 실어 보내면 아무도 읽지 않는 복사본만
 * 늘어난다. 그 설정은 `:app` 이 로그인 상태를 관찰해 SDK 에 직접 알린다 — 도메인을 거칠 이유가 없다.
 *
 * 남은 [currentScreen] 은 SDK 가 대신해줄 수 없다. `ResourceProbe` 와 `DiagnosticPayload` 에는
 * 화면 필드가 없어서 **"어느 화면에서 난 잼/에러인지"를 오직 여기서만 알 수 있다.**
 */
data class ObsContext(
    val currentScreen: ScreenKey?,
)
