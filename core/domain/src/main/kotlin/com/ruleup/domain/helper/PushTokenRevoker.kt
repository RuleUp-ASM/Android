package com.ruleup.domain.helper

/**
 * 이 기기의 푸시 토큰을 서버에서 해제하는 포트(명세 DELETE /api/v1/devices). 구현은 `:app` 이 꽂는다.
 *
 * 로그아웃이 이걸 빠뜨리면 토큰이 이전 계정에 묶인 채 남아, 로그아웃한 기기에 그 계정 푸시가 계속 온다.
 */
fun interface PushTokenRevoker {
    /**
     * 로컬 토큰을 지우기 **전에** 호출한다 — 서버가 요청자로 (유저, 토큰) 쌍을 찾는다.
     *
     * 실패해도 던지지 않는다. 구현이 삼키고, 남은 토큰은 서버가 정리한다.
     */
    suspend fun revoke()
}
