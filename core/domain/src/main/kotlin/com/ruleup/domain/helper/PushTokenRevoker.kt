package com.ruleup.domain.helper

/** 이 기기의 푸시 토큰을 서버에서 해제하는 포트. */
fun interface PushTokenRevoker {
    /** 로컬 토큰을 지우기 전에 호출한다 */
    suspend fun revoke()
}
