package com.ruleup.verification.data.signal.usage

import android.annotation.SuppressLint
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.ruleup.verification.data.db.usage.UsageCursorDao
import com.ruleup.verification.data.db.usage.UsageCursorEntity
import com.ruleup.verification.data.db.usage.UsageEventDao
import com.ruleup.verification.data.db.usage.UsageEventEntity
import com.ruleup.verification.data.db.usage.UsageEventKind
import com.ruleup.verification.data.signal.common.GapRecorder
import com.ruleup.verification.domain.entity.GapReason
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** UsageStats 증분 수집. */
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

            // UsageStats 보존 기간 초과 시 USAGE_PURGED 기록.
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
                if (mapping.kind == UsageEventKind.APP && event.packageName !in targetPackages) continue
                buffer.add(
                    UsageEventEntity(
                        kind = mapping.kind,
                        packageName = if (mapping.kind == UsageEventKind.APP) event.packageName else "",
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

            // UsageStats 이벤트 보존 한계 보수 추정.
            private const val PURGE_THRESHOLD_MS = 5L * 24 * 60 * 60 * 1000
        }
    }
