package com.ruleup.verification.data.sync

import com.ruleup.verification.data.db.common.toDomain
import com.ruleup.verification.data.db.geofence.GeofenceTargetDao
import com.ruleup.verification.domain.entity.SignalScope
import com.ruleup.verification.domain.repository.HealthTargetStore
import com.ruleup.verification.domain.repository.SyncScopeProvider
import com.ruleup.verification.domain.repository.UsageTargetStore
import javax.inject.Inject

/** 활성 챌린지의 신호 수집 범위. */
class SyncScopeProviderImpl
    @Inject
    constructor(
        private val geofenceTargetDao: GeofenceTargetDao,
        private val usageTargetStore: UsageTargetStore,
        private val healthTargetStore: HealthTargetStore,
        private val restoreTargets: com.ruleup.verification.domain.usecase.RestoreVerificationTargetsUseCase,
    ) : SyncScopeProvider {
        override suspend fun currentScope(): SignalScope {
            try {
                return restoreTargets(geofenceTargetDao.all().map { it.toDomain() })
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                return localScope()
            }
        }

        private suspend fun localScope(): SignalScope =
            SignalScope(
                targetPackages = usageTargetStore.all(),
                activeRequestIds = geofenceTargetDao.all().mapTo(HashSet()) { it.requestId },
                healthTargets = healthTargetStore.all(),
                sleepRequested = healthTargetStore.sleepRequested(),
            )
    }
