package com.ruleup.network.auth

/**
 * Refresh Token 으로 토큰을 갱신한다.
 * 성공 시 새 access token 을 반환하고, 실패(재로그인 필요) 시 null 을 반환한다.
 * 구현은 data 레이어에서 바인딩한다.
 */
interface TokenRefresher {
    fun refresh(): String?
}
