package com.ruleup.domain.token

import com.ruleup.domain.entity.user.Token

/**
 * accessToken 만료(401) 시 refreshToken 으로 토큰을 재발급하는 포트.
 */
interface TokenRefresher {
    /**
     * [refreshToken] 으로 새 토큰(회전: access/refresh 둘 다 갱신)을 재발급한다.
     *
     * @return 재발급된 [RefreshedSession]. 세션이 만료돼(refreshToken 도 거절, 401 `SESSION_EXPIRED`) 재발급이
     *   불가능하면 `null`. 일시적 오류(네트워크·5xx 등)는 예외로 전파해, 호출자가 세션을 정리하지 않고
     *   재시도만 포기하도록 한다.
     */
    suspend fun refresh(refreshToken: String): RefreshedSession?
}

/**
 * 갱신 결과.
 *
 * @property userId 서버가 갱신 응답에 함께 내려주는 사용자 식별자. **없으면 null 이다** — 이 필드를
 *   내려주지 않는 배포본이 있을 수 있고, 그때 기존 값을 덮어 비우면 사용자 귀속이 끊긴다.
 */
data class RefreshedSession(
    val token: Token,
    val userId: String?,
)
