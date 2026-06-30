package com.ruleup.verification.data.repository

import com.ruleup.verification.data.db.GeofenceTransitionDao
import com.ruleup.verification.data.db.HealthReadingDao
import com.ruleup.verification.data.db.LocationSampleDao
import com.ruleup.verification.data.db.SignalGapDao
import com.ruleup.verification.data.db.SleepSegmentDao
import com.ruleup.verification.data.db.UsageEventDao
import com.ruleup.verification.data.db.toAppEvent
import com.ruleup.verification.data.db.toDomain
import com.ruleup.verification.data.db.toScreenEvent
import com.ruleup.verification.domain.entity.SignalBatch
import com.ruleup.verification.domain.entity.SignalGap
import com.ruleup.verification.domain.entity.VerificationSignal
import com.ruleup.verification.domain.port.SignalRepository
import timber.log.Timber
import javax.inject.Inject

// 수집·동기화 경로 공통 로그 태그(Worker 와 동일). 디버그 오버레이/Logcat 에서 'VerifySync' 로 필터.
private const val SYNC_LOG_TAG = "VerifySync"

/**
 * Room 기반 로컬 신호 버퍼(명세 §2.4·전송 스펙 §0.5). 드레인은 tagPending(배치키 부여) → byBatch → markSynced 흐름.
 * geofence/location/usage/health/sleep 신호 + signal_gap 공백 버퍼를 한 배치로 묶는다.
 */
class SignalRepositoryImpl
    @Inject
    constructor(
        private val geofenceTransitionDao: GeofenceTransitionDao,
        private val locationSampleDao: LocationSampleDao,
        private val usageEventDao: UsageEventDao,
        private val healthReadingDao: HealthReadingDao,
        private val sleepSegmentDao: SleepSegmentDao,
        private val signalGapDao: SignalGapDao,
    ) : SignalRepository {
        override suspend fun drainPending(collectedAt: String): SignalBatch? {
            // 미드레인 행에 배치키를 부여(드레인 도중 새로 들어온 행은 null 로 남아 다음 배치로).
            geofenceTransitionDao.tagPending(collectedAt)
            locationSampleDao.tagPending(collectedAt)
            usageEventDao.tagPending(collectedAt)
            healthReadingDao.tagPending(collectedAt)
            sleepSegmentDao.tagPending(collectedAt)

            val transitions = geofenceTransitionDao.byBatch(collectedAt)
            val locations = locationSampleDao.byBatch(collectedAt)
            val usage = usageEventDao.byBatch(collectedAt)
            val healthReadings = healthReadingDao.byBatch(collectedAt)
            val sleepSegments = sleepSegmentDao.byBatch(collectedAt)
            // 디버그 가시화(수집 경로): 이번 배치로 드레인된 신호 건수를 타입별로 남긴다(0이면 스코프/권한 미충족).
            Timber.tag(SYNC_LOG_TAG).i(
                "수집 드레인 — geofence=%d, location=%d, usage=%d, health=%d, sleep=%d",
                transitions.size,
                locations.size,
                usage.size,
                healthReadings.size,
                sleepSegments.size,
            )
            if (transitions.isEmpty() &&
                locations.isEmpty() &&
                usage.isEmpty() &&
                healthReadings.isEmpty() &&
                sleepSegments.isEmpty()
            ) {
                return null
            }

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
                    // HEALTH 는 readings 가 귀속 날짜(date)별로 묶인다(보통 오늘 1건).
                    healthReadings
                        .groupBy { it.date }
                        .forEach { (date, rows) ->
                            add(VerificationSignal.Health(date = date, readings = rows.map { it.toDomain() }))
                        }
                    if (sleepSegments.isNotEmpty()) {
                        add(VerificationSignal.Sleep(sleepSegments.map { it.toDomain() }))
                    }
                }
            return SignalBatch(collectedAt = collectedAt, signals = signals)
        }

        override suspend fun drainGaps(collectedAt: String): List<SignalGap> {
            // 신호 배치와 동일한 멱등 키로 공백 버퍼를 묶어 envelope `gaps[]` 로 보낸다.
            signalGapDao.tagPending(collectedAt)
            return signalGapDao.byBatch(collectedAt).map { it.toDomain() }
        }

        override suspend fun markSynced(collectedAt: String) {
            geofenceTransitionDao.markSynced(collectedAt)
            locationSampleDao.markSynced(collectedAt)
            usageEventDao.markSynced(collectedAt)
            healthReadingDao.markSynced(collectedAt)
            sleepSegmentDao.markSynced(collectedAt)
            signalGapDao.markSynced(collectedAt)
        }

        override suspend fun purgeExpired(ttlMillis: Long) {
            val threshold = System.currentTimeMillis() - ttlMillis
            geofenceTransitionDao.purge(threshold)
            locationSampleDao.purge(threshold)
            usageEventDao.purge(threshold)
            healthReadingDao.purge(threshold)
            sleepSegmentDao.purge(threshold)
            signalGapDao.purge(threshold)
        }
    }
