package com.ruleup.verification.data.sync

import com.ruleup.verification.data.db.GeofenceTargetDao
import com.ruleup.verification.domain.entity.SignalScope
import com.ruleup.verification.domain.port.HealthTargetStore
import com.ruleup.verification.domain.port.SyncScopeProvider
import com.ruleup.verification.domain.port.UsageTargetStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * 활성 챌린지 스코프(명세 §3.2): 등록된 지오펜스 requestId(GPS) + 대상 패키지(SCREEN_TIME) +
 * 움직임/수면 대상(HEALTH·SLEEP).
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class SyncScopeProviderImpl(
    private val geofenceTargetDao: GeofenceTargetDao,
    private val usageTargetStore: UsageTargetStore,
    private val healthTargetStore: HealthTargetStore,
) : SyncScopeProvider {
    override suspend fun currentScope(): SignalScope =
        SignalScope(
            targetPackages = usageTargetStore.all(),
            activeRequestIds = geofenceTargetDao.all().mapTo(HashSet()) { it.requestId },
            healthTargets = healthTargetStore.all(),
            sleepRequested = healthTargetStore.sleepRequested(),
        )
}
