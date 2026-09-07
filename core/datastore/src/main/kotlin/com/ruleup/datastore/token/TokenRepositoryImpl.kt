package com.ruleup.datastore.token

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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
 * 토큰 저장소. DataStore 의 `IOException` 을 여기서 끊고 "저장된 게 없다"(= 로그아웃)로 환원한다 —
 * 읽는 쪽(인터셉터 `runBlocking`·`App.onCreate` 의 `first()`)이 예외를 감당하지 못한다.
 */
class TokenRepositoryImpl
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
        private val observability: Observability,
    ) : TokenRepository {
        // 인터셉터가 코루틴 없이 읽어 가는 스냅샷. 저장·조회·삭제마다 같이 갱신한다.
        @Volatile
        private var cachedAccess: String? = null

        /**
         * 읽기 경로의 단일 입구. `IOException` 만 삼킨다 —
         * 취소나 프로그래밍 오류까지 덮으면 진짜 버그가 "로그인 안 됨"으로 위장된다.
         */
        private val preferences: Flow<Preferences> =
            dataStore.data.catch { cause ->
                if (cause !is IOException) throw cause
                observability.w(TAG, cause) { "토큰 저장소 읽기 실패 — 빈 값으로 진행(재로그인 필요)" }
                emit(emptyPreferences())
            }

        override val isLoggedIn: Flow<Boolean> =
            preferences.map { prefs -> prefs[KEY_REFRESH] != null }

        // 갱신 응답이 userId 를 안 주는 배포본에서는 비어 있을 수 있다 — 사용자 귀속이 필요한 쪽은 이 Flow 를 본다.
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
                prefs[KEY_EVER_LOGGED_IN] = true
            }
        }

        override suspend fun saveTokens(
            token: Token,
            userId: String?,
        ) {
            cachedAccess = token.accessToken
            write("saveTokens") { prefs ->
                prefs[KEY_ACCESS] = token.accessToken
                prefs[KEY_REFRESH] = token.refreshToken
                // 갱신 응답이 userId 를 안 주는 배포본에서 null 로 덮으면 사용자 귀속이 끊긴다.
                userId?.let { prefs[KEY_USER_ID] = it }
                // 갱신 성공 = 이 기기에 세션이 있었다는 뜻이라, 플래그가 생기기 전에 로그인한 사용자도 여기서 채워진다.
                prefs[KEY_EVER_LOGGED_IN] = true
            }
        }

        override suspend fun getAccessToken(): String? =
            preferences
                .map { it[KEY_ACCESS] }
                .first()
                .also { cachedAccess = it }

        override fun cachedAccessToken(): String? = cachedAccess

        override suspend fun getRefreshToken(): String? = preferences.map { it[KEY_REFRESH] }.first()

        override suspend fun getUserId(): String? = preferences.map { it[KEY_USER_ID] }.first()

        override suspend fun hasEverLoggedIn(): Boolean = preferences.map { it[KEY_EVER_LOGGED_IN] == true }.first()

        override suspend fun clear() {
            cachedAccess = null
            write("clear") { prefs ->
                // 로그인 이력만 남긴다. 지우면 재로그인이 첫 설치로 집계된다.
                val everLoggedIn = prefs[KEY_EVER_LOGGED_IN]
                prefs.clear()
                everLoggedIn?.let { prefs[KEY_EVER_LOGGED_IN] = it }
            }
        }

        /**
         * 쓰기 경로의 단일 입구. `IOException` 을 호출부로 전파하지 않는다 —
         * 전파하면 `AutoLoginUseCase` 의 진입 판정이 끝나지 않아 스플래시에서 멈춘다.
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
            private val KEY_EVER_LOGGED_IN = booleanPreferencesKey("has_ever_logged_in")
        }
    }
