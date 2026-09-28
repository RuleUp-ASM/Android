package com.ruleup.domain.navigation

/** 한 경로가 로그인을 요구하는지 판단한다. */
fun interface RouteAccessPolicy {
    /** [path] 화면을 열려면 로그인이 필요한가. */
    fun requiresLogin(path: String): Boolean
}
