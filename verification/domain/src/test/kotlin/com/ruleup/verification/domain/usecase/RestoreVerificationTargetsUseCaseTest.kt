package com.ruleup.verification.domain.usecase

import com.ruleup.challenge.domain.entity.ChallengeDetail
import com.ruleup.challenge.domain.entity.ChallengeGate
import com.ruleup.challenge.domain.entity.ChallengeMode
import com.ruleup.challenge.domain.entity.ChallengePeriod
import com.ruleup.challenge.domain.entity.ChallengeStats
import com.ruleup.challenge.domain.entity.ChallengeStatus
import com.ruleup.challenge.domain.entity.JoinNote
import com.ruleup.challenge.domain.entity.MemberRole
import com.ruleup.challenge.domain.entity.MyChallenge
import com.ruleup.challenge.domain.entity.MyChallengePage
import com.ruleup.challenge.domain.entity.OwnerType
import com.ruleup.challenge.domain.entity.VerificationConfig
import com.ruleup.challenge.domain.entity.VerificationMethod
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.challenge.domain.fake.FakeChallengeRepository
import com.ruleup.domain.test.FakeTokenRepository
import com.ruleup.verification.domain.entity.GeofenceTarget
import com.ruleup.verification.domain.entity.HealthMetric
import com.ruleup.verification.domain.entity.HealthTarget
import com.ruleup.verification.domain.entity.MyScreenApps
import com.ruleup.verification.domain.entity.PendingScreenApps
import com.ruleup.verification.domain.entity.ScreenApp
import com.ruleup.verification.domain.repository.GeofenceRegister
import com.ruleup.verification.domain.repository.HealthTargetStore
import com.ruleup.verification.domain.repository.UsageTargetStore
import com.ruleup.verification.domain.test.FakeVerificationRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RestoreVerificationTargetsUseCaseTest {
    @Test
    fun `전체 건강 권한 이름으로 걸음과 거리 수집 대상을 복원한다`() =
        runTest {
            val health = HealthStore()
            val useCase =
                RestoreVerificationTargetsUseCase(
                    repository(
                        VerificationMethod.HEALTH,
                        listOf("android.permission.health.READ_STEPS", "android.permission.health.READ_DISTANCE"),
                    ),
                    FakeVerificationRepository(),
                    FakeTokenRepository(storedUserId = "me"),
                    Fences(),
                    health,
                    UsageStore(),
                )
            val expected = setOf(HealthTarget(HealthMetric.STEPS, null), HealthTarget(HealthMetric.DISTANCE, null))
            assertEquals(expected, useCase(emptyList()).healthTargets)
            assertEquals(expected, health.targets)
        }

    @Test
    fun `비어 있는 로컬 저장소에 건강과 수면 대상을 복원한다`() =
        runTest {
            val health = HealthStore()
            val useCase =
                RestoreVerificationTargetsUseCase(
                    repository(VerificationMethod.HEALTH, listOf("READ_STEPS", "READ_SLEEP")),
                    FakeVerificationRepository(),
                    FakeTokenRepository(storedUserId = "me"),
                    Fences(),
                    health,
                    UsageStore(),
                )
            val scope = useCase(emptyList())
            assertEquals(setOf(HealthTarget(HealthMetric.STEPS, null)), health.targets)
            assertTrue(scope.sleepRequested)
            assertEquals(setOf("c1"), scope.activeChallengeIds)
        }

    @Test
    fun `오늘 적용된 앱만 복원하고 내일 변경은 수집하지 않는다`() =
        runTest {
            val usage = UsageStore()
            val useCase =
                RestoreVerificationTargetsUseCase(
                    repository(VerificationMethod.SCREEN_TIME_MAX),
                    FakeVerificationRepository(myScreenApps = {
                        MyScreenApps(
                            apps = listOf(ScreenApp("old", "현재")),
                            appliedFrom = null,
                            pending = PendingScreenApps(listOf(ScreenApp("new", "내일")), "2026-09-29T00:00:00+09:00"),
                        )
                    }),
                    FakeTokenRepository(storedUserId = "me"),
                    Fences(),
                    HealthStore(),
                    usage,
                )
            assertEquals(setOf("old"), useCase(emptyList()).targetPackages)
            assertEquals(mapOf("c1" to setOf("old")), usage.targets)
        }

    @Test
    fun `챌린지가 여럿이면 조회 순서와 무관하게 참여 목록 순서대로 모은다`() =
        runTest {
            val ids = (1..6).map { "c$it" }
            val usage = UsageStore()
            val useCase =
                RestoreVerificationTargetsUseCase(
                    repository(VerificationMethod.SCREEN_TIME_MAX, ids = ids),
                    FakeVerificationRepository(myScreenApps = { id ->
                        MyScreenApps(apps = listOf(ScreenApp("app.$id", id)), appliedFrom = null, pending = null)
                    }),
                    FakeTokenRepository(storedUserId = "me"),
                    Fences(),
                    HealthStore(),
                    usage,
                )
            val scope = useCase(emptyList())
            assertEquals(ids, usage.targets.keys.toList())
            assertEquals(ids.toSet(), scope.activeChallengeIds)
        }

    @Test
    fun `서버 조회 실패로 기존 로컬 대상을 지우지 않는다`() =
        runTest {
            val health = HealthStore().apply { targets = setOf(HealthTarget(HealthMetric.DISTANCE, null)) }
            val useCase =
                RestoreVerificationTargetsUseCase(
                    FakeChallengeRepository(myChallenges = { _, _ -> throw java.io.IOException() }),
                    FakeVerificationRepository(),
                    FakeTokenRepository(storedUserId = "me"),
                    Fences(),
                    health,
                    UsageStore(),
                )
            assertFailsWith<java.io.IOException> { useCase(emptyList()) }
            assertEquals(setOf(HealthTarget(HealthMetric.DISTANCE, null)), health.targets)
        }

    private fun repository(
        method: VerificationMethod,
        permissions: List<String> = emptyList(),
        ids: List<String> = listOf("c1"),
    ) = FakeChallengeRepository(
        myChallenges = { _, _ ->
            MyChallengePage(
                ids.map { id ->
                    MyChallenge(
                        challengeId = id,
                        title = "루틴",
                        description = null,
                        imageUrl = null,
                        category = null,
                        mode = ChallengeMode.SOLO,
                        visibility = null,
                        status = ChallengeStatus.ACTIVE,
                        participantCount = 1,
                        capacity = 1,
                        minTier = null,
                        weeklyCount = 7,
                        period = ChallengePeriod("2026-09-01", "2026-10-01"),
                        myRole = MemberRole.OWNER,
                        ownerType = OwnerType.USER,
                        leftType = null,
                        leftAt = null,
                        successRate = null,
                    )
                },
                null,
                false,
            )
        },
        detail = { id ->
            ChallengeDetail(
                title = "루틴",
                category = null,
                imageUrl = null,
                challengeId = id,
                description = null,
                mode = ChallengeMode.SOLO,
                visibility = null,
                status = ChallengeStatus.ACTIVE,
                owner = null,
                ownerType = OwnerType.USER,
                participantCount = 1,
                capacity = 1,
                isFull = false,
                period = ChallengePeriod("2026-09-01", "2026-10-01"),
                verification = VerificationConfig(VerificationType.AUTO, method, requiredPermissions = permissions),
                stats = ChallengeStats(null, null),
                gate = ChallengeGate(null, null, true),
                joinBlockReason = null,
                rejoinAvailableAt = null,
                joinNote = JoinNote.IMMEDIATE,
                cloneable = false,
                myRole = MemberRole.OWNER,
                moderation = null,
            )
        },
    )

    private class HealthStore : HealthTargetStore {
        var targets = emptySet<HealthTarget>()

        override suspend fun replaceAll(
            targets: Set<HealthTarget>,
            sleepRequested: Boolean,
        ) {
            this.targets = targets
        }

        override suspend fun all() = targets

        override suspend fun sleepRequested() = false
    }

    private class UsageStore : UsageTargetStore {
        var targets = emptyMap<String, Set<String>>()

        override suspend fun replaceAll(targets: Map<String, Set<String>>) {
            this.targets = targets
        }

        override suspend fun replaceFor(
            challengeId: String,
            packages: Set<String>,
        ) {
            targets = targets + (challengeId to packages)
        }

        override suspend fun all() = targets.values.flatten().toSet()
    }

    private class Fences : GeofenceRegister {
        override suspend fun reconcile(targets: List<GeofenceTarget>) = Unit

        override suspend fun reconcilePersisted() = Unit

        override suspend fun bind(
            requestIdPrefix: String,
            targets: List<GeofenceTarget>,
        ) = Unit

        override suspend fun unbind(requestIdPrefix: String) = Unit

        override suspend fun clear() = Unit
    }
}
