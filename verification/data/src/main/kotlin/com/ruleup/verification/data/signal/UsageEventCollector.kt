package com.ruleup.verification.data.signal

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.ruleup.verification.data.db.KIND_APP
import com.ruleup.verification.data.db.UsageCursorDao
import com.ruleup.verification.data.db.UsageCursorEntity
import com.ruleup.verification.data.db.UsageEventDao
import com.ruleup.verification.data.db.UsageEventEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * UsageStats 증분 수집(명세 §2.2). 매 sync 마다 직전 커서~now 구간을 queryEvents 로 읽어
 * Room usage_event 에 누적한다(시스템 보존 한계 대응, 마감 시점 몰아 조회 금지).
 * 누적 foregroundSec 단일값은 만들지 않고 RESUMED/PAUSED 시퀀스를 그대로 보존한다.
 */
class UsageEventCollector
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val usageEventDao: UsageEventDao,
        private val usageCursorDao: UsageCursorDao,
    ) {
        suspend fun collect(targetPackages: Set<String>) {
            if (!context.hasUsageAccess()) return
            val manager = context.getSystemService(UsageStatsManager::class.java) ?: return

            val now = System.currentTimeMillis()
            val begin = usageCursorDao.get()?.lastQueriedAt ?: (now - INITIAL_WINDOW_MS)
            if (begin >= now) {
                usageCursorDao.set(UsageCursorEntity(lastQueriedAt = now))
                return
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
        }
    }
