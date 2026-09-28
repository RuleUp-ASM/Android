package com.ruleup.notification.domain.fake

import com.ruleup.notification.domain.entity.NotificationPage
import com.ruleup.notification.domain.entity.NotificationSettings
import com.ruleup.notification.domain.entity.NotificationSettingsResult
import com.ruleup.notification.domain.entity.NotificationSettingsUpdate
import com.ruleup.notification.domain.entity.NotificationTab
import com.ruleup.notification.domain.entity.UnreadSummary
import com.ruleup.notification.domain.repository.NotificationRepository

/** 테스트용 [NotificationRepository]. */
class FakeNotificationRepository(
    private val page: ((String?) -> NotificationPage)? = null,
    private val unread: (() -> UnreadSummary)? = null,
    private val settings: (() -> NotificationSettings)? = null,
    private val update: ((NotificationSettingsUpdate) -> NotificationSettingsResult)? = null,
    // 음소거 실패를 재현할 때만 준다.
    private val mute: ((String, Boolean) -> Unit)? = null,
) : NotificationRepository {
    val calls = mutableListOf<String>()

    /** 어떤 커서로 물었는지. */
    val cursors = mutableListOf<String?>()

    /** 어떤 탭을 물었는지. */
    val tabs = mutableListOf<NotificationTab>()

    /** 읽음 처리에 보낸 값. */
    val readMarkers = mutableListOf<Pair<NotificationTab, String>>()

    val updates = mutableListOf<NotificationSettingsUpdate>()

    val mutes = mutableListOf<Pair<String, Boolean>>()

    override suspend fun getNotifications(
        tab: NotificationTab,
        cursor: String?,
    ): NotificationPage {
        calls += "getNotifications"
        cursors += cursor
        tabs += tab
        return requireNotNull(page) { "getNotifications 를 준비하지 않았다" }(cursor)
    }

    override suspend fun markRead(
        tab: NotificationTab,
        lastNotificationId: String,
    ) {
        calls += "markRead"
        readMarkers += tab to lastNotificationId
    }

    override suspend fun getUnreadSummary(): UnreadSummary {
        calls += "getUnreadSummary"
        return unread?.invoke() ?: UnreadSummary.EMPTY
    }

    override suspend fun getSettings(): NotificationSettings {
        calls += "getSettings"
        return requireNotNull(settings) { "getSettings 를 준비하지 않았다" }()
    }

    override suspend fun updateSettings(update: NotificationSettingsUpdate): NotificationSettingsResult {
        calls += "updateSettings"
        updates += update
        return requireNotNull(this.update) { "updateSettings 를 준비하지 않았다" }(update)
    }

    override suspend fun setMuted(
        challengeId: String,
        muted: Boolean,
    ) {
        calls += "setMuted"
        mute?.invoke(challengeId, muted)
        mutes += challengeId to muted
    }
}
