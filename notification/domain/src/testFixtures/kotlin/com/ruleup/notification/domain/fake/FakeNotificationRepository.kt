package com.ruleup.notification.domain.fake

import com.ruleup.notification.domain.entity.NotificationPage
import com.ruleup.notification.domain.entity.NotificationSettings
import com.ruleup.notification.domain.entity.NotificationSettingsResult
import com.ruleup.notification.domain.entity.NotificationSettingsUpdate
import com.ruleup.notification.domain.entity.NotificationTab
import com.ruleup.notification.domain.entity.UnreadSummary
import com.ruleup.notification.domain.repository.NotificationRepository

/**
 * 테스트용 [NotificationRepository]. 준비하지 않은 메서드는 호출되면 실패한다 —
 * 화면이 의도치 않은 조회를 해도 조용히 지나가지 않게 하려는 것이다.
 *
 * 미읽음 집계는 **레드닷을 쓰는 화면이 전부 부수 조회로 부르므로** 기본값을 둔다. 준비하지
 * 않았다고 홈 테스트가 줄줄이 깨지면 그 화면의 진짜 계약이 가려진다.
 */
class FakeNotificationRepository(
    private val page: ((String?) -> NotificationPage)? = null,
    private val unread: (() -> UnreadSummary)? = null,
    private val settings: (() -> NotificationSettings)? = null,
    private val update: ((NotificationSettingsUpdate) -> NotificationSettingsResult)? = null,
    // 음소거 실패를 재현할 때만 준다. 기본은 성공(서버가 멱등 204 를 준다)
    private val mute: ((String, Boolean) -> Unit)? = null,
) : NotificationRepository {
    val calls = mutableListOf<String>()

    /** 어떤 커서로 물었는지. 첫 페이지는 null 이다. */
    val cursors = mutableListOf<String?>()

    /** 어떤 탭을 물었는지. 공지 탭이 알림 탭과 섞이지 않는지 보는 자리다. */
    val tabs = mutableListOf<NotificationTab>()

    /** 읽음 처리에 보낸 값. 응답에 담겼던 최신 id 여야 한다. */
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
