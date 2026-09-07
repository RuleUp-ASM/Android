package com.ruleup.verification.data.repository

import com.ruleup.observability.domain.api.Observability
import com.ruleup.observability.domain.api.i
import com.ruleup.verification.data.db.common.SignalGapDao
import com.ruleup.verification.data.db.common.toAppEvent
import com.ruleup.verification.data.db.common.toDomain
import com.ruleup.verification.data.db.geofence.GeofenceTransitionDao
import com.ruleup.verification.data.db.geofence.LocationSampleDao
import com.ruleup.verification.data.db.health.HealthReadingDao
import com.ruleup.verification.data.db.health.SleepSessionDao
import com.ruleup.verification.data.db.usage.UsageEventDao
import com.ruleup.verification.data.signal.usage.WakeSignalProvider
import com.ruleup.verification.domain.entity.SignalBatch
import com.ruleup.verification.domain.entity.SignalGap
import com.ruleup.verification.domain.entity.VerificationSignal
import com.ruleup.verification.domain.repository.SignalRepository
import javax.inject.Inject

// 수집·동기화 경로 공통 로그 태그(Worker 와 동일). 디버그 오버레이/Logcat 에서 'VerifySync' 로 필터.
private const val SYNC_LOG_TAG = "VerifySync"

// 상세 로그 폭주 방지: 타입별 최대 이만큼만 값까지 찍고 나머지는 "…외 N건" 으로 줄인다.
private const val MAX_DETAIL_PER_TYPE = 30

// line(it) 은 로그 게이트를 통과한 뒤에만 평가된다 — Timber 시절의 treeCount 가드가 필요 없어졌다.
private fun <T> List<T>.logSignalDetail(
    observability: Observability,
    type: String,
    line: (T) -> String,
) {
    if (isEmpty()) return
    take(MAX_DETAIL_PER_TYPE).forEach { observability.i(SYNC_LOG_TAG) { "  · ${line(it)}" } }
    if (size > MAX_DETAIL_PER_TYPE) {
        observability.i(SYNC_LOG_TAG) { "  · $type …외 ${size - MAX_DETAIL_PER_TYPE}건" }
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
        private val sleepSessionDao: SleepSessionDao,
        private val signalGapDao: SignalGapDao,
        private val wakeSignalProvider: WakeSignalProvider,
        private val observability: Observability,
    ) : SignalRepository {
        override suspend fun drainPending(collectedAt: String): SignalBatch? {
            // 미전송 행 전부에 이번 배치키를 부여한다 — 앞 배치가 실패해 키만 남은 행도 다시 싣는다(#319).
            geofenceTransitionDao.tagPending(collectedAt)
            locationSampleDao.tagPending(collectedAt)
            usageEventDao.tagPending(collectedAt)
            healthReadingDao.tagPending(collectedAt)
            sleepSessionDao.tagPending(collectedAt)

            val transitions = geofenceTransitionDao.byBatch(collectedAt)
            val locations = locationSampleDao.byBatch(collectedAt)
            val usage = usageEventDao.byBatch(collectedAt)
            val healthReadings = healthReadingDao.byBatch(collectedAt)
            val sleepSessions = sleepSessionDao.byBatch(collectedAt)
            // 건수가 0이면 스코프가 비었거나 권한이 없다는 뜻이다.
            observability.i(SYNC_LOG_TAG) {
                "수집 드레인 — geofence=${transitions.size}, location=${locations.size}, " +
                    "usage=${usage.size}, health=${healthReadings.size}, sleep=${sleepSessions.size}"
            }
            transitions.logSignalDetail(observability, "geofence") {
                "geofence ${it.transition} req=${it.requestId} at=${it.occurredAt} acc=${it.accuracy}m mock=${it.isMock}"
            }
            locations.logSignalDetail(observability, "location") {
                "location (${"%.5f".format(it.lat)}, ${"%.5f".format(it.lng)}) acc=${it.accuracy}m mock=${it.isMock}"
            }
            usage.logSignalDetail(observability, "usage") { "usage ${it.kind}/${it.eventType} ${it.packageName}" }
            healthReadings.logSignalDetail(observability, "health") {
                "health ${it.metric}=${it.value} ${it.date} via ${it.originPackage}"
            }
            sleepSessions.logSignalDetail(observability, "sleep") {
                "sleep ${(it.durationMillis) / 60_000}m (실수면 ${it.sleepMillis?.div(60_000) ?: "미상"}m) via ${it.originPackage}"
            }
            if (transitions.isEmpty() &&
                locations.isEmpty() &&
                usage.isEmpty() &&
                healthReadings.isEmpty() &&
                sleepSessions.isEmpty()
            ) {
                return null
            }

            val appEvents = usage.mapNotNull { it.toAppEvent() }
            // WAKE 는 배치가 아니라 당일 전체에서 뽑는다 — 첫 잠금해제는 하루 한 번뿐이라
            // 그 이벤트가 앞선 배치로 나갔으면 이후 sync 에서 값이 사라진다.
            val wake = wakeSignalProvider.collect()

            val signals =
                buildList<VerificationSignal> {
                    if (transitions.isNotEmpty()) {
                        add(VerificationSignal.GeofenceTransitions(transitions.map { it.toDomain() }))
                    }
                    if (appEvents.isNotEmpty()) {
                        add(VerificationSignal.ScreenTime(appEvents = appEvents))
                    }
                    if (wake != null) add(wake)
                    if (locations.isNotEmpty()) {
                        add(VerificationSignal.Locations(locations.map { it.toDomain() }))
                    }
                    // metric 이 신호 레벨 필드라(전송 스펙 §2) 날짜뿐 아니라 metric 으로도 갈라 묶는다.
                    healthReadings
                        .groupBy { it.date to it.metric }
                        .forEach { (key, rows) ->
                            val (date, metric) = key
                            add(
                                VerificationSignal.Health(
                                    date = date,
                                    metric = metric,
                                    readings = rows.map { it.toDomain() },
                                ),
                            )
                        }
                    if (sleepSessions.isNotEmpty()) {
                        add(VerificationSignal.Sleep(sleepSessions.map { it.toDomain() }))
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
            sleepSessionDao.markSynced(collectedAt)
            signalGapDao.markSynced(collectedAt)
        }

        override suspend fun purgeExpired(ttlMillis: Long) {
            val threshold = System.currentTimeMillis() - ttlMillis
            geofenceTransitionDao.purge(threshold)
            locationSampleDao.purge(threshold)
            usageEventDao.purge(threshold)
            healthReadingDao.purge(threshold)
            sleepSessionDao.purge(threshold)
            signalGapDao.purge(threshold)
        }
    }
