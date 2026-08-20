package com.ruleup.verification.domain.usecase

import com.ruleup.domain.entity.user.Token
import com.ruleup.domain.token.TokenRepository
import com.ruleup.verification.domain.entity.GeofenceTarget
import com.ruleup.verification.domain.entity.LocationPin
import com.ruleup.verification.domain.repository.GeofenceRegistrar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class BindLocationUseCaseTest {
    @Test
    fun `앵커 전체를 userId#challengeId#index requestId 로 bind 한다`() =
        runBlocking {
            val registrar = FakeRegistrar()

            BindLocationUseCase(registrar, FakeTokenRepository(storedUserId = "u1"))(
                challengeId = "c1",
                anchors =
                    listOf(
                        LocationPin(lat = 37.0, lng = 127.0, label = "헬스장"),
                        LocationPin(lat = 37.1, lng = 127.1, label = "공원"),
                    ),
                radiusM = 500f,
                dwellMinutes = 60,
            )

            assertEquals("u1#c1", registrar.boundPrefix)
            assertEquals(listOf("u1#c1#0", "u1#c1#1"), registrar.bound.map { it.requestId })
            // 반경은 앵커별이 아니라 서버 설정 단일값이다(인증 정책 §1.1).
            assertEquals(listOf(500f, 500f), registrar.bound.map { it.radiusM })
            assertEquals(60, registrar.bound.first().dwellMinutes)
        }

    @Test
    fun `userId 미저장 세션은 challengeId 접두로 폴백한다`() =
        runBlocking {
            val registrar = FakeRegistrar()

            BindLocationUseCase(registrar, FakeTokenRepository(storedUserId = null))(
                challengeId = "c1",
                anchors = listOf(LocationPin(lat = 37.0, lng = 127.0, label = null)),
                radiusM = 500f,
                dwellMinutes = 60,
            )

            assertEquals("c1", registrar.boundPrefix)
            assertEquals(listOf("c1#0"), registrar.bound.map { it.requestId })
        }

    private class FakeRegistrar : GeofenceRegistrar {
        var boundPrefix: String? = null
        var bound: List<GeofenceTarget> = emptyList()

        override suspend fun reconcile(targets: List<GeofenceTarget>) = Unit

        override suspend fun reconcilePersisted() = Unit

        override suspend fun bind(
            requestIdPrefix: String,
            targets: List<GeofenceTarget>,
        ) {
            boundPrefix = requestIdPrefix
            bound = targets
        }

        override suspend fun unbind(requestIdPrefix: String) = Unit

        override suspend fun clear() = Unit
    }

    private class FakeTokenRepository(
        private val storedUserId: String?,
    ) : TokenRepository {
        override suspend fun saveSession(
            token: Token,
            userId: String,
        ) = Unit

        override suspend fun saveTokens(
            token: Token,
            userId: String?,
        ) = Unit

        override suspend fun getAccessToken(): String? = null

        override fun cachedAccessToken(): String? = null

        override suspend fun getRefreshToken(): String? = null

        override suspend fun getUserId(): String? = storedUserId

        override suspend fun hasEverLoggedIn(): Boolean = storedUserId != null

        override suspend fun clear() = Unit

        override val isLoggedIn: Flow<Boolean> = flowOf(storedUserId != null)

        override val userId: Flow<String?> = flowOf(storedUserId)
    }
}
