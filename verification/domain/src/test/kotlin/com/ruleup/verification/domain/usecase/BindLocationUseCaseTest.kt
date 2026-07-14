package com.ruleup.verification.domain.usecase

import com.ruleup.verification.domain.entity.GeofenceTarget
import com.ruleup.verification.domain.entity.LocationPin
import com.ruleup.verification.domain.repository.GeofenceRegistrar
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class BindLocationUseCaseTest {
    @Test
    fun `앵커 전체를 인덱스 requestId 로 bind 한다`() =
        runBlocking {
            val registrar = FakeRegistrar()

            BindLocationUseCase(registrar)(
                challengeMemberId = "m1",
                anchors =
                    listOf(
                        LocationPin(lat = 37.0, lng = 127.0, radiusM = 600f, label = "헬스장"),
                        LocationPin(lat = 37.1, lng = 127.1, radiusM = 800f, label = "공원"),
                    ),
                dwellMinutes = 60,
            )

            assertEquals("m1", registrar.boundPrefix)
            assertEquals(listOf("m1#0", "m1#1"), registrar.bound.map { it.requestId })
            assertEquals(listOf(600f, 800f), registrar.bound.map { it.radiusM })
            assertEquals(60, registrar.bound.first().dwellMinutes)
        }

    @Test
    fun `반경은 명세 범위로 클램프해 bind 한다`() =
        runBlocking {
            val registrar = FakeRegistrar()

            BindLocationUseCase(registrar)(
                challengeMemberId = "m2",
                anchors =
                    listOf(
                        LocationPin(lat = 37.0, lng = 127.0, radiusM = 999_999f, label = null),
                        LocationPin(lat = 37.1, lng = 127.1, radiusM = 5f, label = null),
                    ),
                dwellMinutes = 30,
            )

            assertEquals(GeofenceTarget.MAX_RADIUS_M, registrar.bound[0].radiusM)
            assertEquals(GeofenceTarget.MIN_RADIUS_M, registrar.bound[1].radiusM)
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
}
