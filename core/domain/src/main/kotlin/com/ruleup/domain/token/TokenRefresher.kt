package com.ruleup.domain.token

import com.ruleup.domain.entity.user.Token

/** accessToken 만료(401) 시 refreshToken 으로 토큰을 재발급하는 포트. */
interface TokenRefresher {
    /** [refreshToken] 으로 재발급한 토큰은 반환 전에 저장한다. 세션이 바뀌면 null 을 반환한다. */
    suspend fun refresh(refreshToken: String): RefreshedSession?
}

/** 갱신 결과. */
data class RefreshedSession(
    val token: Token,
    val userId: String?,
)
