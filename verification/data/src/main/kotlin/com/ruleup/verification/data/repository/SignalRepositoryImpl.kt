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

// 상세 로그 폭주 방지: 타입별 최대 이만큼만 값까지 찍고 나머지는 "…외 N건" 으로 줄인다.
private const val MAX_DETAIL_PER_TYPE = 30

// 드레인된 행의 실제 값을 한 줄씩 'VerifySync' 로 남긴다(디버그 오버레이/Logcat). 릴리스는 트리 미식재라 무시된다.
private fun <T> List<T>.logSignalDetail(
    type: String,
    line: (T) -> String,
) {
    // line(it) 는 String.format 을 포함해 즉시 평가되므로, 심을 트리가 없으면(릴리스) 아예 만들지 않는다.
    if (isEmpty() || Timber.treeCount == 0) return
    take(MAX_DETAIL_PER_TYPE).forEach { Timber.tag(SYNC_LOG_TAG).i("  · %s", line(it)) }
    if (size > MAX_DETAIL_PER_TYPE) {
        Timber.tag(SYNC_LOG_TAG).i("  · %s …외 %d건", type, size - MAX_DETAIL_PER_TYPE)
    }
}

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
            // 건수 다음으로 실제 값까지 한 줄씩(타입별 최대 30건). 어느 패키지·좌표·헬스 수치·전이인지 눈으로 확인.
            transitions.logSignalDetail("geofence") {
                "geofence ${it.transition} req=${it.requestId} " +
                    "(${"%.5f".format(it.lat)}, ${"%.5f".format(it.lng)}) acc=${it.accuracy}m mock=${it.isMock}"
            }
            locations.logSignalDetail("location") {
                "location (${"%.5f".format(it.lat)}, ${"%.5f".format(it.lng)}) acc=${it.accuracy}m mock=${it.isMock}"
            }
            usage.logSignalDetail("usage") { "usage ${it.kind}/${it.eventType} ${it.packageName}" }
            healthReadings.logSignalDetail("health") {
                "health ${it.metric}=${it.value}${it.unit}" +
                    (it.exerciseType?.let { e -> " ($e)" }.orEmpty()) +
                    " ${it.date} via ${it.dataOrigin}"
            }
            sleepSegments.logSignalDetail("sleep") {
                "sleep ${it.status} ${(it.endAt - it.startAt) / 60_000}m"
            }
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
