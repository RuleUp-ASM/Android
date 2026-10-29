package com.ruleup.verification.data.signal.geofence

import com.ruleup.verification.domain.entity.GeofenceTarget
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GeofenceReconcileTest {
    @Test
    fun `desired 에서 빠진 requestId 만 제거 대상이다`() {
        val previous = setOf("a", "b", "c")
        val targets = listOf(target("b"), target("d"))

        val toRemove = GeofenceReconcile.toRemove(previous, targets)

        assertEquals(setOf("a", "c"), toRemove.toSet())
    }

    @Test
    fun `목표가 비면 직전 전체가 제거 대상이다`() {
        val previous = setOf("a", "b")

        val toRemove = GeofenceReconcile.toRemove(previous, emptyList())

        assertEquals(setOf("a", "b"), toRemove.toSet())
    }

    @Test
    fun `직전이 비면 제거 대상이 없다 - 재부팅 후 전체 재등록`() {
        val toRemove = GeofenceReconcile.toRemove(emptySet(), listOf(target("a"), target("b")))

        assertTrue(toRemove.isEmpty())
    }

    @Test
    fun `같은 지오펜스로 반복 동기화하면 등록은 한 번만 한다`() =
        runBlocking {
            val registered = mutableMapOf<String, String>()
            val added = mutableListOf<List<GeofenceTarget>>()
            val coordinator =
                GeofenceRegistrationCoordinator(
                    readRegistered = { registered.toMap() },
                    writeRegistered = {
                        registered.clear()
                        registered.putAll(it)
                    },
                    add = {
                        added += it
                        true
                    },
                    remove = { true },
                )
            val desired = listOf(target("a"), target("b"))

            coordinator.reconcile(desired)
            coordinator.reconcile(desired)

            assertEquals(listOf(desired), added)
        }

    @Test
    fun `등록에 실패한 지오펜스는 다음 동기화에서 다시 시도한다`() =
        runBlocking {
            val registered = mutableMapOf<String, String>()
            var calls = 0
            val coordinator =
                GeofenceRegistrationCoordinator(
                    readRegistered = { registered.toMap() },
                    writeRegistered = {
                        registered.clear()
                        registered.putAll(it)
                    },
                    add = { ++calls > 1 },
                    remove = { true },
                )

            coordinator.reconcile(listOf(target("a")))
            coordinator.reconcile(listOf(target("a")))

            assertEquals(2, calls)
        }

    @Test
    fun `일부 장소만 바뀌면 바뀐 지오펜스만 다시 등록한다`() =
        runBlocking {
            val registered = mutableMapOf<String, String>()
            val added = mutableListOf<List<GeofenceTarget>>()
            val coordinator =
                GeofenceRegistrationCoordinator(
                    readRegistered = { registered.toMap() },
                    writeRegistered = {
                        registered.clear()
                        registered.putAll(it)
                    },
                    add = {
                        added += it
                        true
                    },
                    remove = { true },
                )
            val unchanged = target("a")
            val updated = target("b").copy(radiusM = 200f)
            coordinator.reconcile(listOf(unchanged, target("b")))
            coordinator.reconcile(listOf(unchanged, updated))

            assertEquals(listOf(updated), added.last())
        }

    @Test
    fun `변경된 장소 등록에 실패하면 기존 등록 기록을 보존하고 변경이 유지될 때 재시도한다`() =
        runBlocking {
            val registered = mutableMapOf<String, String>()
            val added = mutableListOf<List<GeofenceTarget>>()
            val coordinator =
                GeofenceRegistrationCoordinator(
                    readRegistered = { registered.toMap() },
                    writeRegistered = {
                        registered.clear()
                        registered.putAll(it)
                    },
                    add = {
                        added += it
                        added.size != 2
                    },
                    remove = { true },
                )
            val original = target("a")
            val changed = original.copy(radiusM = 200f)

            coordinator.reconcile(listOf(original))
            val originalRegistration = registered.toMap()
            coordinator.reconcile(listOf(changed))
            assertEquals(originalRegistration, registered)
            coordinator.reconcile(listOf(original))
            assertEquals(2, added.size)
            coordinator.reconcile(listOf(changed))

            assertEquals(listOf(listOf(original), listOf(changed), listOf(changed)), added)
            assertTrue(originalRegistration != registered)
        }

    @Test
    fun `권한이 없을 때는 등록하지 않고 복구되면 미확인 장소를 등록한다`() =
        runBlocking {
            val registered = mutableMapOf("a" to "")
            var calls = 0
            val coordinator =
                GeofenceRegistrationCoordinator(
                    readRegistered = { registered.toMap() },
                    writeRegistered = {
                        registered.clear()
                        registered.putAll(it)
                    },
                    add = {
                        calls++
                        true
                    },
                    remove = { true },
                )

            coordinator.reconcile(listOf(target("a")), canRegister = false)
            assertEquals(0, calls)
            coordinator.reconcile(listOf(target("a")))
            assertEquals(1, calls)
        }

    @Test
    fun `삭제에 실패한 지오펜스는 다음 동기화에서 삭제를 재시도한다`() =
        runBlocking {
            val registered = mutableMapOf("a" to "old")
            var calls = 0
            val coordinator =
                GeofenceRegistrationCoordinator(
                    readRegistered = { registered.toMap() },
                    writeRegistered = {
                        registered.clear()
                        registered.putAll(it)
                    },
                    add = { true },
                    remove = { ++calls > 1 },
                )

            coordinator.reconcile(emptyList())
            assertEquals(setOf("a"), registered.keys)
            coordinator.reconcile(emptyList())
            assertTrue(registered.isEmpty())
            assertEquals(2, calls)
        }

    @Test
    fun `좌표와 반경과 체류 시간 변경은 각각 새 등록으로 반영된다`() =
        runBlocking {
            val original = target("a")
            val changes =
                listOf(
                    original.copy(lat = 1.0),
                    original.copy(lng = 1.0),
                    original.copy(radiusM = 200f),
                    original.copy(dwellMinutes = 30),
                )
            changes.forEach { changed ->
                val registered = mutableMapOf<String, String>()
                val added = mutableListOf<List<GeofenceTarget>>()
                val coordinator =
                    GeofenceRegistrationCoordinator(
                        readRegistered = { registered.toMap() },
                        writeRegistered = {
                            registered.clear()
                            registered.putAll(it)
                        },
                        add = {
                            added += it
                            true
                        },
                        remove = { true },
                    )
                coordinator.reconcile(listOf(original))
                coordinator.reconcile(listOf(changed))
                assertEquals(listOf(changed), added.last())
                assertEquals(2, added.size)
            }
        }

    private fun target(id: String): GeofenceTarget = GeofenceTarget(requestId = id, lat = 0.0, lng = 0.0, radiusM = 100f, dwellMinutes = 60)
}
