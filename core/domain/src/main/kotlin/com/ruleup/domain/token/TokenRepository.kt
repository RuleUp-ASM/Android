package com.ruleup.domain.token

import com.ruleup.entity.user.Token
import kotlinx.coroutines.flow.Flow

interface TokenRepository {
    suspend fun saveTokens(token: Token)

    suspend fun getAccessToken(): String?

    /**
     * 마지막으로 알려진 accessToken 의 인메모리 스냅샷(동기). 저장/조회/삭제 시 갱신된다.
     * 아직 워밍되지 않았으면 null. OkHttp 인터셉터가 매 요청 DataStore 를 블로킹 조회하지 않도록 쓴다.
     */
    fun cachedAccessToken(): String?

    suspend fun getRefreshToken(): String?

    /** 로그인/가입 시 저장한 내 userId. 멤버 단위 로컬 키(지오펜스 requestId 등) 파생에 쓴다. */
    suspend fun saveUserId(userId: String)

    /** 저장된 내 userId. 로그인/가입 전(또는 저장 이전 구버전 세션)이면 null. */
    suspend fun getUserId(): String?

    /**
     * 저장된 userId 를 reactive 로 관찰한다. 미저장이면 null 을 방출한다.
     *
     * [isLoggedIn] 대신 이걸 구독해야 하는 경우가 있다. 로그인 경로가
     * `saveTokens` → `saveUserId` 순서라 **[isLoggedIn] 이 true 로 바뀌는 시점엔 userId 가 아직
     * 비어 있다.** 관측 시스템의 사용자 귀속처럼 "userId 가 실제로 존재할 때"가 필요하면 이쪽을 쓴다.
     */
    val userId: Flow<String?>

    suspend fun clear()

    val isLoggedIn: Flow<Boolean>
}
