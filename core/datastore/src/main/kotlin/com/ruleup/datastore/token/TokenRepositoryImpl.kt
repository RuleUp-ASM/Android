package com.ruleup.datastore.token

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ruleup.domain.entity.user.Token
import com.ruleup.domain.token.TokenRepository
import com.ruleup.observability.domain.api.Observability
import com.ruleup.observability.domain.api.e
import com.ruleup.observability.domain.api.w
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

private const val TAG = "TokenStore"

/**
 * 토큰 저장소. **저장소 고장이 앱 고장이 되지 않게** 하는 것이 이 구현의 절반이다.
 *
 * DataStore 의 읽기·쓰기는 `IOException` 을 던진다. 그런데 이 저장소를 읽는 자리들이 하나같이
 * 예외를 감당하지 못한다 — OkHttp 인터셉터의 `runBlocking`, `App.onCreate` 의 `first()`,
 * 세션 종료를 구독하는 컴포지션 스코프. 그래서 여기서 끊고 **"저장된 게 없다"로 환원**한다.
 *
 * 환원의 의미는 로그아웃이다. 사용자는 다시 로그인하면 되고, 그건 재설치보다 훨씬 낫다.
 * 다만 조용히 넘기면 빈도를 알 수 없으므로 전부 관측 채널로 올린다.
 *
 * 파일 손상 자체의 복구는 `DataStoreModule` 의 corruptionHandler 가 맡는다 — 그쪽은 파일을
 * 갈아엎고, 이쪽은 그 외의 읽기·쓰기 실패를 덮는다.
 */
class TokenRepositoryImpl
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
        private val observability: Observability,
    ) : TokenRepository {
        // 인터셉터의 동기 조회용 accessToken 스냅샷. 저장/조회/삭제 시 갱신한다.
        @Volatile
        private var cachedAccess: String? = null

        /**
         * 읽기 경로의 단일 입구. 모든 조회가 이걸 거친다.
         *
         * `IOException` 만 삼킨다 — 취소나 프로그래밍 오류까지 덮으면 진짜 버그가
         * "로그인 안 됨"으로 위장된다.
         */
        private val preferences: Flow<Preferences> =
            dataStore.data.catch { cause ->
                if (cause !is IOException) throw cause
                observability.w(TAG, cause) { "토큰 저장소 읽기 실패 — 빈 값으로 진행(재로그인 필요)" }
                emit(emptyPreferences())
            }

        // refreshToken 존재 여부로 로그인 상태를 반영하는 reactive Flow.
        override val isLoggedIn: Flow<Boolean> =
            preferences.map { prefs -> prefs[KEY_REFRESH] != null }

        // userId 자체를 관찰한다. isLoggedIn 은 saveTokens 시점에 true 가 되지만 userId 는
        // 그 다음 edit 에서 써지므로, 사용자 귀속이 필요한 쪽은 이 Flow 를 봐야 한다.
        override val userId: Flow<String?> =
            preferences.map { prefs -> prefs[KEY_USER_ID] }

        override suspend fun saveSession(
            token: Token,
            userId: String,
        ) {
            // 캐시를 먼저 채운다 — 디스크 쓰기가 실패해도 이번 실행 동안의 세션은 살아 있다.
            cachedAccess = token.accessToken
            // 한 번의 edit 이라 원자적이다. 나눠 쓰면 그 사이에 isLoggedIn 만 true 인 구간이 생긴다.
            write("saveSession") { prefs ->
                prefs[KEY_ACCESS] = token.accessToken
                prefs[KEY_REFRESH] = token.refreshToken
                prefs[KEY_USER_ID] = userId
            }
        }

        override suspend fun saveTokens(token: Token) {
            cachedAccess = token.accessToken
            write("saveTokens") { prefs ->
                prefs[KEY_ACCESS] = token.accessToken
                prefs[KEY_REFRESH] = token.refreshToken
            }
        }

        override suspend fun getAccessToken(): String? =
            preferences
                .map { it[KEY_ACCESS] }
                .first()
                .also { cachedAccess = it }

        override fun cachedAccessToken(): String? = cachedAccess

        override suspend fun getRefreshToken(): String? = preferences.map { it[KEY_REFRESH] }.first()

        override suspend fun saveUserId(userId: String) {
            write("saveUserId") { prefs -> prefs[KEY_USER_ID] = userId }
        }

        override suspend fun getUserId(): String? = preferences.map { it[KEY_USER_ID] }.first()

        override suspend fun clear() {
            cachedAccess = null
            write("clear") { it.clear() }
        }

        /**
         * 쓰기 경로의 단일 입구. `IOException` 을 **호출부로 전파하지 않는다.**
         *
         * 전파하면 `AutoLoginUseCase` 가 `saveTokens` 를 `runCatching` 밖(`fold` 의 `onSuccess`)에서
         * 부르는 탓에 `SessionBootstrap` 의 판정이 끝나지 않고 **스플래시에서 멈춘다.** 저장에 실패한
         * 세션은 다음 실행에서 로그아웃으로 나타나므로, 여기서는 기록만 남기고 흐름을 살린다.
         */
        private suspend fun write(
            op: String,
            block: (MutablePreferences) -> Unit,
        ) {
            try {
                dataStore.edit { block(it) }
            } catch (e: IOException) {
                observability.e(TAG, e) { "토큰 저장소 쓰기 실패: $op — 다음 실행에서 로그아웃된다" }
            }
        }

        companion object {
            private val KEY_ACCESS = stringPreferencesKey("access_token")
            private val KEY_REFRESH = stringPreferencesKey("refresh_token")
            private val KEY_USER_ID = stringPreferencesKey("user_id")
        }
    }
