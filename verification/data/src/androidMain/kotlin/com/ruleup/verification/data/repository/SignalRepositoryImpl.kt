package com.ruleup.verification.data.repository

import com.ruleup.verification.data.db.GeofenceTransitionDao
import com.ruleup.verification.data.db.LocationSampleDao
import com.ruleup.verification.data.db.UsageEventDao
import com.ruleup.verification.data.db.toAppEvent
import com.ruleup.verification.data.db.toDomain
import com.ruleup.verification.data.db.toScreenEvent
import com.ruleup.verification.domain.entity.SignalBatch
import com.ruleup.verification.domain.entity.VerificationSignal
import com.ruleup.verification.domain.port.SignalRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * Room 기반 로컬 신호 버퍼(명세 §2.4). 드레인은 tagPending(배치키 부여) → byBatch → markSynced 흐름.
 * geofence_transition / location_sample 두 버퍼를 한 배치로 묶는다(usage_event 는 Phase 3 에서 합류).
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class SignalRepositoryImpl(
    private val geofenceTransitionDao: GeofenceTransitionDao,
    private val locationSampleDao: LocationSampleDao,
    private val usageEventDao: UsageEventDao,
) : SignalRepository {
    override suspend fun drainPending(collectedAt: String): SignalBatch? {
        // 미드레인 행에 배치키를 부여(드레인 도중 새로 들어온 행은 null 로 남아 다음 배치로).
        geofenceTransitionDao.tagPending(collectedAt)
        locationSampleDao.tagPending(collectedAt)
        usageEventDao.tagPending(collectedAt)

        val transitions = geofenceTransitionDao.byBatch(collectedAt)
        val locations = locationSampleDao.byBatch(collectedAt)
        val usage = usageEventDao.byBatch(collectedAt)
        if (transitions.isEmpty() && locations.isEmpty() && usage.isEmpty()) return null

        val appEvents = usage.mapNotNull { it.toAppEvent() }
        val screenEvents = usage.mapNotNull { it.toScreenEvent() }

        val signals =
            buildList<VerificationSignal> {
                if (transitions.isNotEmpty()) {
                    add(VerificationSignal.GeofenceTransitions(transitions.map { it.toDomain() }))
                }
                if (appEvents.isNotEmpty() || screenEvents.isNotEmpty()) {
                    add(VerificationSignal.ScreenTime(appEvents = appEvents, screenEvents = screenEvents))
                }
                if (locations.isNotEmpty()) {
                    add(VerificationSignal.Locations(locations.map { it.toDomain() }))
                }
            }
        return SignalBatch(collectedAt = collectedAt, signals = signals)
    }

    override suspend fun markSynced(collectedAt: String) {
        geofenceTransitionDao.markSynced(collectedAt)
        locationSampleDao.markSynced(collectedAt)
        usageEventDao.markSynced(collectedAt)
    }

    override suspend fun purgeExpired(ttlMillis: Long) {
        val threshold = System.currentTimeMillis() - ttlMillis
        geofenceTransitionDao.purge(threshold)
        locationSampleDao.purge(threshold)
        usageEventDao.purge(threshold)
    }
}
