package com.ruleup.domain.token

import com.ruleup.domain.entity.user.Token
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
     * 토큰을 회전시킨다. 갱신 경로 전용이다 — 로그인·가입은 [saveSession] 을 쓴다.
     *
     * [userId] 가 null 이면 **기존 값을 그대로 둔다.** 갱신 응답이 이 필드를 안 내려주는 배포본에서
     * 덮어 비우면 사용자 귀속이 끊기기 때문이다.
     */
    suspend fun saveTokens(
        token: Token,
        userId: String? = null,
    )

    suspend fun getAccessToken(): String?

    /**
     * 마지막으로 알려진 accessToken 의 인메모리 스냅샷(동기). 저장/조회/삭제 시 갱신된다.
     * 아직 워밍되지 않았으면 null. OkHttp 인터셉터가 매 요청 DataStore 를 블로킹 조회하지 않도록 쓴다.
     */
    fun cachedAccessToken(): String?

    suspend fun getRefreshToken(): String?

    /** 저장된 내 userId. 로그인/가입 전(또는 저장 이전 구버전 세션)이면 null. */
    suspend fun getUserId(): String?

    /**
     * 저장된 userId 를 reactive 로 관찰한다. 미저장이면 null 을 방출한다.
     *
     * 로그인은 [saveSession] 으로, 갱신은 [saveTokens] 로 userId 를 함께 저장하므로 보통은
     * [isLoggedIn] 과 함께 채워진다. 다만 갱신 응답이 userId 를 안 주는 배포본에서는 비어 있을 수
     * 있다 — 사용자 귀속처럼 "userId 가 실제로 존재할 때"가 필요하면 [isLoggedIn] 이 아니라 이쪽을
     * 구독한다.
     */
    val userId: Flow<String?>

    suspend fun clear()

    /**
     * 이 기기에서 한 번이라도 로그인에 성공한 적이 있는지. **[clear] 로 지워지지 않는다.**
     *
     * 로그인 화면이 "첫 설치"와 "재로그인"을 가르는 근거다 — 완주율의 분모라 둘을 섞으면 지표가
     * 무의미해진다. 부팅 시점의 토큰 유무로 대신하면 로그아웃 상태에서 앱을 완전히 종료한 사용자가
     * 다음 실행에서 첫 설치로 집계된다.
     */
    suspend fun hasEverLoggedIn(): Boolean

    val isLoggedIn: Flow<Boolean>
}
