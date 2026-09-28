package com.ruleup.domain.navigation

/** 서버가 준 딥링크 문자열을 앱 라우트로 옮긴다. */
fun interface DeeplinkResolver {
    /** 해석할 수 없으면 null */
    fun resolve(deeplink: String): NavRoute?
}
