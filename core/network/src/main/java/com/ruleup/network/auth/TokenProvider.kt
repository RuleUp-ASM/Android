package com.ruleup.network.auth

/**
 * 현재 저장된 access token 을 동기로 제공한다.
 * OkHttp 인터셉터/Authenticator 가 네트워크 스레드에서 호출하므로 blocking 시그니처다.
 * 구현(토큰 저장소 접근)은 data 레이어에서 바인딩한다.
 */
interface TokenProvider {
    /** 현재 access token. 없으면 null. */
    fun accessToken(): String?
}
