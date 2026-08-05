package com.ruleup.datastore.token

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.ruleup.domain.entity.user.Token
import com.ruleup.observability.domain.event.DiagnosticPayload
import com.ruleup.observability.domain.model.Severity
import com.ruleup.observability.domain.test.RecordingSink
import com.ruleup.observability.domain.test.testObservability
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * 저장소 고장이 앱 고장으로 번지지 않는지 검증한다.
 *
 * 실제 `DataStore` 대신 대역을 쓴다 — 파일 손상·디스크 오류를 결정적으로 재현해야 하는데,
 * 진짜 파일로는 그걸 안정적으로 만들 수 없다.
 */
class TokenRepositoryImplTest {
    private fun token(
        access: String = "a1",
        refresh: String = "r1",
    ) = Token(accessToken = access, refreshToken = refresh, tokenType = "Bearer", expiresInSeconds = 3600)

    private val sink = RecordingSink()
    private val observability = testObservability(sink = sink)

    private fun repo(dataStore: DataStore<Preferences>) = TokenRepositoryImpl(dataStore, observability)

    private val diagnostics get() = sink.payloads.filterIsInstance<DiagnosticPayload>()

    // ---------- 정상 동작 ----------

    @Test
    fun `저장한 토큰을 다시 읽는다`() =
        runTest {
            val repo = repo(FakeDataStore())

            repo.saveTokens(token())

            assertEquals("a1", repo.getAccessToken())
            assertEquals("r1", repo.getRefreshToken())
            assertTrue(repo.isLoggedIn.first())
        }

    @Test
    fun `로그아웃해도 로그인 이력은 남는다`() =
        runTest {
            // 로그인 화면이 첫 설치와 재로그인을 가르는 근거다. 지워지면 재로그인이 첫 설치로
            // 집계돼 완주율의 분모가 뒤섞인다.
            val repo = repo(FakeDataStore())
            repo.saveSession(token(), userId = "u-1")

            repo.clear()

            assertNull(repo.getRefreshToken())
            assertTrue(repo.hasEverLoggedIn())
        }

    @Test
    fun `한 번도 로그인한 적이 없으면 이력이 없다`() =
        runTest {
            assertFalse(repo(FakeDataStore()).hasEverLoggedIn())
        }

    @Test
    fun `saveSession 은 토큰과 userId 를 한 번에 저장한다`() =
        runTest {
            val store = CountingDataStore()
            val repo = repo(store)

            repo.saveSession(token(), userId = "u-1")

            assertEquals("r1", repo.getRefreshToken())
            assertEquals("u-1", repo.getUserId())
            // 쓰기가 나뉘면 그 사이에 isLoggedIn 만 true 인 구간이 생긴다.
            assertEquals(1, store.writeCount)
        }

    @Test
    fun `saveTokens 는 userId 를 안 주면 저장된 값을 지우지 않는다`() =
        runTest {
            val repo = repo(FakeDataStore())
            repo.saveSession(token(), userId = "u-1")

            // 서버가 userId 를 안 내려주는 배포본. 덮어 비우면 사용자 귀속이 끊긴다.
            repo.saveTokens(token(access = "a2", refresh = "r2"))

            assertEquals("r2", repo.getRefreshToken())
            assertEquals("u-1", repo.getUserId())
        }

    @Test
    fun `saveTokens 는 userId 를 주면 함께 저장한다`() =
        runTest {
            val repo = repo(FakeDataStore())

            // 갱신 응답이 userId 를 함께 준다. 이것만으로 세션이 완성돼야 별도 조회가 필요 없다.
            repo.saveTokens(token(), userId = "u-1")

            assertEquals("u-1", repo.getUserId())
        }

    @Test
    fun `clear 는 저장값과 캐시를 함께 비운다`() =
        runTest {
            val repo = repo(FakeDataStore())
            repo.saveTokens(token())

            repo.clear()

            assertNull(repo.cachedAccessToken())
            assertNull(repo.getRefreshToken())
            assertFalse(repo.isLoggedIn.first())
        }

    // ---------- 읽기 실패 ----------

    @Test
    fun `읽기가 IOException 이면 빈 값으로 환원하고 앱은 로그아웃 상태가 된다`() =
        runTest {
            val repo = repo(ThrowingDataStore(IOException("disk gone")))

            assertNull(repo.getAccessToken())
            assertNull(repo.getRefreshToken())
            assertNull(repo.getUserId())
            assertFalse(repo.isLoggedIn.first())
        }

    @Test
    fun `읽기 실패는 관측 채널로 올라간다`() =
        runTest {
            val repo = repo(ThrowingDataStore(IOException("disk gone")))

            repo.getAccessToken()

            val logged = diagnostics.single()
            assertEquals(Severity.WARN, logged.severity)
            assertEquals("TokenStore", logged.tag)
            assertEquals("java.io.IOException", logged.cause?.type)
        }

    @Test
    fun `IOException 이 아닌 예외는 삼키지 않는다`() =
        runTest {
            val repo = repo(ThrowingDataStore(IllegalStateException("bug")))

            val thrown = runCatching { repo.getAccessToken() }.exceptionOrNull()

            assertTrue("프로그래밍 오류가 '로그아웃' 으로 위장되면 안 된다", thrown is IllegalStateException)
        }

    // ---------- 쓰기 실패 ----------

    @Test
    fun `쓰기가 실패해도 호출부로 전파되지 않는다`() =
        runTest {
            val repo = repo(FakeDataStore(failWritesWith = IOException("no space")))

            // 전파되면 진입 판정이 끝나지 않아 스플래시에서 멈춘다.
            repo.saveTokens(token())
            repo.clear()
        }

    @Test
    fun `쓰기 실패는 ERROR 로 기록된다`() =
        runTest {
            val repo = repo(FakeDataStore(failWritesWith = IOException("no space")))

            repo.saveTokens(token())

            val logged = diagnostics.single()
            assertEquals(Severity.ERROR, logged.severity)
            assertTrue(logged.message.contains("saveTokens"))
        }

    @Test
    fun `쓰기가 실패해도 이번 실행의 세션은 캐시로 살아 있다`() =
        runTest {
            val repo = repo(FakeDataStore(failWritesWith = IOException("no space")))

            repo.saveTokens(token())

            // 인터셉터는 cachedAccessToken 을 먼저 본다 — 디스크가 죽어도 이번 실행은 인증된다.
            assertEquals("a1", repo.cachedAccessToken())
        }
}

/** 메모리 위에서 도는 DataStore 대역. [failWritesWith] 를 채우면 쓰기가 실패한다. */
private class FakeDataStore(
    private val failWritesWith: Throwable? = null,
) : DataStore<Preferences> {
    private val state = MutableStateFlow<Preferences>(emptyPreferences())

    override val data: Flow<Preferences> get() = state

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
        failWritesWith?.let { throw it }
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}

/** 쓰기 횟수를 세는 대역. 원자성(한 번의 edit) 검증용. */
private class CountingDataStore : DataStore<Preferences> {
    private val state = MutableStateFlow<Preferences>(emptyPreferences())
    var writeCount = 0
        private set

    override val data: Flow<Preferences> get() = state

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
        writeCount++
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}

/** 읽기가 항상 [cause] 로 실패하는 대역. 손상 파일·디스크 오류 재현용. */
private class ThrowingDataStore(
    private val cause: Throwable,
) : DataStore<Preferences> {
    override val data: Flow<Preferences> get() = flow { throw cause }

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = mutablePreferencesOf()
}
