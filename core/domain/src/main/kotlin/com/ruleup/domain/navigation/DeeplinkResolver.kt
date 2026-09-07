package com.ruleup.domain.navigation

/**
 * 서버가 준 딥링크 문자열을 앱 라우트로 옮긴다.
 *
 * 계약은 core 에 두고 구현은 `:app` 이 갖는다 — 라우트 표(`appRoutes`)를 아는 건 컴포지션 루트뿐이고,
 * feature 가 그걸 알면 알림 타입이 늘 때마다 feature 를 고쳐야 한다.
 *
 * 알림 딥링크는 전부 커스텀 스킴 `ruleup://` 이다(알림 테크 스펙 8) — 앱 내부 소비 전용이라
 * https 앱링크를 쓰지 않아 웹에 노출되지 않는다.
 */
fun interface DeeplinkResolver {
    /** 해석할 수 없으면 null — 호출부가 폴백(목록 유지)을 정한다. */
    fun resolve(deeplink: String): NavRoute?
}
