package com.ruleup.domain.token

import com.ruleup.domain.entity.user.Token

/**
 * accessToken 만료(401) 시 refreshToken 으로 토큰을 재발급하는 포트.
 *
 * 구현(AuthApi 호출)은 onboarding:data 에 둔다.
 */
interface TokenRefresher {
    /**
     * [refreshToken] 으로 새 토큰(회전: access/refresh 둘 다 갱신)을 재발급한다.
     *
     * @return 재발급된 [Token]. 세션이 만료돼(refreshToken 도 거절, 401 `SESSION_EXPIRED`) 재발급이
     *   불가능하면 `null`. 일시적 오류(네트워크·5xx 등)는 예외로 전파해, 호출자가 세션을 정리하지 않고
     *   재시도만 포기하도록 한다.
     */
    suspend fun refresh(refreshToken: String): Token?
}
