package com.ruleup.verification.data.signal

import android.annotation.SuppressLint
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.ruleup.verification.data.db.KIND_APP
import com.ruleup.verification.data.db.UsageCursorDao
import com.ruleup.verification.data.db.UsageCursorEntity
import com.ruleup.verification.data.db.UsageEventDao
import com.ruleup.verification.data.db.UsageEventEntity
import com.ruleup.verification.domain.entity.GapReason
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * UsageStats 증분 수집(명세 §2.2). 매 sync 마다 직전 커서~now 구간을 queryEvents 로 읽어
 * Room usage_event 에 누적한다(시스템 보존 한계 대응, 마감 시점 몰아 조회 금지).
 * 누적 foregroundSec 단일값은 만들지 않고 RESUMED/PAUSED 시퀀스를 그대로 보존한다.
 *
 * queryEvents(PACKAGE_USAGE_STATS)는 UsageStats 접근 권한 가드 뒤에서만 호출되므로 lint 를 억제한다.
 */
@SuppressLint("MissingPermission")
class UsageEventCollector
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val usageEventDao: UsageEventDao,
        private val usageCursorDao: UsageCursorDao,
        private val gapRecorder: GapRecorder,
    ) {
        suspend fun collect(targetPackages: Set<String>) {
            if (!context.hasUsageAccess()) return
            val manager = context.getSystemService(UsageStatsManager::class.java) ?: return

            val now = System.currentTimeMillis()
            val cursor = usageCursorDao.get()?.lastQueriedAt
            val begin = cursor ?: (now - INITIAL_WINDOW_MS)
            if (begin >= now) {
                usageCursorDao.set(UsageCursorEntity(lastQueriedAt = now))
                return
            }

            // OS UsageStats purge 대응(전송 스펙 §3.1·§4.1): 커서가 OS 보존 한계를 넘었으면
            // 그 구간 일부는 소실됐을 수 있어 USAGE_PURGED(복구 불가)로 진단한다.
            if (cursor != null && now - begin > PURGE_THRESHOLD_MS) {
                val lostTo = now - PURGE_THRESHOLD_MS
                gapRecorder.record("SCREEN_TIME", GapReason.USAGE_PURGED, begin, lostTo, recoverable = false)
                gapRecorder.record("WAKE", GapReason.USAGE_PURGED, begin, lostTo, recoverable = false)
            }

            val events = manager.queryEvents(begin, now)
            val buffer = ArrayList<UsageEventEntity>()
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val mapping = usageMappingOf(event.eventType) ?: continue
                // 앱 사용 이벤트는 대상 패키지만(WAKE/화면 이벤트는 패키지 무관 전부).
                if (mapping.kind == KIND_APP && event.packageName !in targetPackages) continue
                buffer.add(
                    UsageEventEntity(
                        kind = mapping.kind,
                        packageName = if (mapping.kind == KIND_APP) event.packageName else "",
                        eventType = mapping.eventType,
                        occurredAt = event.timeStamp,
                    ),
                )
            }
            if (buffer.isNotEmpty()) usageEventDao.insertAll(buffer)
            usageCursorDao.set(UsageCursorEntity(lastQueriedAt = now))
        }

        companion object {
            // 첫 수집 시 당일 첫 잠금해제까지 포착하도록 24시간 윈도우로 시작.
            private const val INITIAL_WINDOW_MS = 24L * 60 * 60 * 1000

            // UsageStats 이벤트 보존 한계 보수 추정. 커서가 이보다 오래되면 일부 purge 가정.
            private const val PURGE_THRESHOLD_MS = 5L * 24 * 60 * 60 * 1000
        }
    }
