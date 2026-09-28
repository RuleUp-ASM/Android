package com.ruleup.verification.domain.usecase

import com.ruleup.challenge.domain.entity.MyChallengeFilter
import com.ruleup.challenge.domain.entity.VerificationMethod
import com.ruleup.challenge.domain.repository.ChallengeRepository
import com.ruleup.domain.token.TokenRepository
import com.ruleup.verification.domain.entity.GeofenceTarget
import com.ruleup.verification.domain.entity.HealthMetric
import com.ruleup.verification.domain.entity.HealthTarget
import com.ruleup.verification.domain.entity.PermissionSnapshot
import com.ruleup.verification.domain.entity.SignalScope
import com.ruleup.verification.domain.repository.GeofenceRegister
import com.ruleup.verification.domain.repository.HealthTargetStore
import com.ruleup.verification.domain.repository.UsageTargetStore
import com.ruleup.verification.domain.repository.VerificationRepository
import javax.inject.Inject

/** 서버 바인딩으로 수집 대상을 복원한다. */
class RestoreVerificationTargetsUseCase
    @Inject
    constructor(
        private val challenges: ChallengeRepository,
        private val verification: VerificationRepository,
        private val tokens: TokenRepository,
        private val geofences: GeofenceRegister,
        private val health: HealthTargetStore,
        private val usage: UsageTargetStore,
    ) {
        suspend operator fun invoke(existing: List<GeofenceTarget>): SignalScope {
            val userId = tokens.getUserId() ?: return SignalScope(emptySet(), emptySet())
            val ids = linkedSetOf<String>()
            val cursors = mutableSetOf<String>()
            var cursor: String? = null
            do {
                val page = challenges.getMyChallenges(MyChallengeFilter.IN_PROGRESS, cursor)
                ids.addAll(page.challenges.map { it.challengeId })
                cursor = if (page.hasNext) requireNotNull(page.nextCursor) else null
                check(cursor == null || cursors.add(cursor))
            } while (cursor != null)
            val locations = mutableListOf<GeofenceTarget>()
            val apps = mutableMapOf<String, Set<String>>()
            val metrics = mutableSetOf<HealthTarget>()
            var sleep = false
            for (id in ids) {
                val config = challenges.getChallenge(id).verification
                if (!config.type.isAuto) continue
                when (config.method) {
                    VerificationMethod.GPS_PRESENCE, VerificationMethod.GPS_AVOID -> {
                        val location = verification.getMyLocation(id) ?: continue
                        location.anchors.forEachIndexed { index, pin ->
                            val requestId = "$userId#$id#$index"
                            val previous = existing.firstOrNull { it.requestId == requestId }
                            locations +=
                                GeofenceTarget(
                                    requestId,
                                    pin.lat,
                                    pin.lng,
                                    location.serverRadiusM ?: previous?.radiusM ?: error("인증 반경이 없어요"),
                                    previous?.dwellMinutes ?: 60,
                                )
                        }
                    }
                    VerificationMethod.SCREEN_TIME_MAX, VerificationMethod.SCREEN_TIME_MIN -> {
                        apps[id] =
                            verification
                                .getMyScreenApps(id)
                                ?.apps
                                .orEmpty()
                                .mapTo(linkedSetOf()) { it.packageName }
                    }
                    VerificationMethod.HEALTH, VerificationMethod.WAKE, VerificationMethod.SLEEP -> {
                        config.requiredPermissions.forEach { permission ->
                            when (PermissionSnapshot.normalizeToken(permission)) {
                                "READ_STEPS", "HEALTH_STEPS", "HEALTH" -> metrics += HealthTarget(HealthMetric.STEPS, null)
                                "READ_DISTANCE", "HEALTH_DISTANCE" -> metrics += HealthTarget(HealthMetric.DISTANCE, null)
                                "READ_SLEEP", "HEALTH_SLEEP", "SLEEP" -> sleep = true
                            }
                        }
                        if (config.method == VerificationMethod.SLEEP) sleep = true
                    }
                    VerificationMethod.SELF_CHECK -> Unit
                }
            }
            check(tokens.getUserId() == userId)
            geofences.reconcile(locations)
            health.replaceAll(metrics, sleep)
            usage.replaceAll(apps)
            return SignalScope(apps.values.flatten().toSet(), locations.mapTo(linkedSetOf()) { it.requestId }, metrics, sleep, ids)
        }
    }
