package com.ruleup.domain.token

import com.ruleup.entity.user.Token
import kotlinx.coroutines.flow.Flow

interface TokenRepository {
    /**
     * 로그인·가입 완료. 토큰과 [userId] 를 **한 번의 쓰기로** 저장한다.
     *
     * 나눠 쓰면 그 사이가 `isLoggedIn=true` 인데 userId 는 없는 구간이 되고, 사용자 귀속이 필요한
     * 쪽이 빈 값을 본다. 로그인 경로는 userId 를 이미 손에 들고 있으므로 나눌 이유가 없다.
     */
    suspend fun saveSession(
        token: Token,
        userId: String,
    )

    /**
     * 토큰만 회전시킨다. 갱신 응답(`TokenRefreshResponse`)에는 userId 가 없으므로 그쪽 전용이다.
     * 로그인·가입은 [saveSession] 을 쓴다.
     */
    suspend fun saveTokens(token: Token)

    suspend fun getAccessToken(): String?

    /**
     * 마지막으로 알려진 accessToken 의 인메모리 스냅샷(동기). 저장/조회/삭제 시 갱신된다.
     * 아직 워밍되지 않았으면 null. OkHttp 인터셉터가 매 요청 DataStore 를 블로킹 조회하지 않도록 쓴다.
     */
    fun cachedAccessToken(): String?

    suspend fun getRefreshToken(): String?

    /**
     * userId 만 따로 채운다. **백필 전용이다.**
     *
     * 로그인·가입은 [saveSession] 을 쓴다. 이 함수가 남아 있는 건 갱신 응답에 userId 가 없어서,
     * userId 없이 저장된 세션을 나중에 프로필 조회로 메워야 하기 때문이다.
     */
    suspend fun saveUserId(userId: String)

    /** 저장된 내 userId. 로그인/가입 전(또는 저장 이전 구버전 세션)이면 null. */
    suspend fun getUserId(): String?

    /**
     * 저장된 userId 를 reactive 로 관찰한다. 미저장이면 null 을 방출한다.
     *
     * 로그인은 [saveSession] 으로 원자적이지만, **[isLoggedIn] 과 항상 일치하지는 않는다** —
     * userId 없이 저장된 옛 세션이 갱신으로 복구되면 백필이 끝날 때까지 비어 있다. 사용자 귀속처럼
     * "userId 가 실제로 존재할 때"가 필요하면 [isLoggedIn] 이 아니라 이쪽을 구독한다.
     */
    val userId: Flow<String?>

    suspend fun clear()

    val isLoggedIn: Flow<Boolean>
}
