package com.ruleup.verification.domain.usecase

import com.ruleup.verification.domain.entity.GeofenceTarget
import com.ruleup.verification.domain.repository.GeofenceRegistrar
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class BindLocationUseCaseTest {
    @Test
    fun `과대 반경은 1000m 로 클램프해 bind 한다`() =
        runBlocking {
            val registrar = FakeRegistrar()

            BindLocationUseCase(registrar)("m1", lat = 37.0, lng = 127.0, radiusM = 5000f, dwellMinutes = 60)

            val target = registrar.bound!!
            assertEquals("m1", target.requestId)
            assertEquals(1000f, target.radiusM)
            assertEquals(60, target.dwellMinutes)
        }

    @Test
    fun `과소 반경은 50m 로 클램프한다`() =
        runBlocking {
            val registrar = FakeRegistrar()

            BindLocationUseCase(registrar)("m2", lat = 37.0, lng = 127.0, radiusM = 5f, dwellMinutes = 30)

            assertEquals(50f, registrar.bound!!.radiusM)
        }

    private class FakeRegistrar : GeofenceRegistrar {
        var bound: GeofenceTarget? = null

        override suspend fun reconcile(targets: List<GeofenceTarget>) = Unit

        override suspend fun bind(target: GeofenceTarget) {
            bound = target
        }

        override suspend fun unbind(requestId: String) = Unit

        override suspend fun clear() = Unit
    }
}
