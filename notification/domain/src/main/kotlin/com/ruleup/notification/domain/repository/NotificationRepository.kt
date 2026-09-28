package com.ruleup.notification.domain.repository

import com.ruleup.notification.domain.entity.NotificationPage
import com.ruleup.notification.domain.entity.NotificationSettings
import com.ruleup.notification.domain.entity.NotificationSettingsResult
import com.ruleup.notification.domain.entity.NotificationSettingsUpdate
import com.ruleup.notification.domain.entity.NotificationTab
import com.ruleup.notification.domain.entity.UnreadSummary

/** 알림 센터와 푸시 설정. */
interface NotificationRepository {
    /** 알림 센터 한 페이지. */
    suspend fun getNotifications(
        tab: NotificationTab = NotificationTab.NOTIFICATION,
        cursor: String? = null,
    ): NotificationPage

    /** 읽음 지점 갱신. */
    suspend fun markRead(
        tab: NotificationTab,
        lastNotificationId: String,
    )

    /** 레드닷·챌린지 카운터용 미읽음 집계. */
    suspend fun getUnreadSummary(): UnreadSummary

    suspend fun getSettings(): NotificationSettings

    /** 보낸 필드만 바꾼다. */
    suspend fun updateSettings(update: NotificationSettingsUpdate): NotificationSettingsResult

    /** 챌린지별 음소거 등록·해제. */
    suspend fun setMuted(
        challengeId: String,
        muted: Boolean,
    )
}
