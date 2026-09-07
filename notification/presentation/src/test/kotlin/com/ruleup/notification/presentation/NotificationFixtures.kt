package com.ruleup.notification.presentation

import com.ruleup.notification.domain.entity.Notification
import com.ruleup.notification.domain.entity.NotificationGroupSettings
import com.ruleup.notification.domain.entity.NotificationPage
import com.ruleup.notification.domain.entity.NotificationSettings
import com.ruleup.notification.domain.entity.NotificationType

/** 알림 한 줄. 테스트 본문에는 그 테스트가 신경 쓰는 값만 넘긴다. */
internal fun notification(
    id: String = "n1",
    type: NotificationType? = NotificationType.VERIFICATION_RESULT,
    title: String = "오늘 판정 완료",
    body: String? = "기상 챌린지 · 연속 7일",
    deeplink: String? = "ruleup://challenge/c_301",
    challengeId: String? = "c_301",
    createdAt: String = "2026-09-04T00:05:00+09:00",
) = Notification(
    id = id,
    type = type,
    title = title,
    body = body,
    deeplink = deeplink,
    challengeId = challengeId,
    createdAt = createdAt,
)

internal fun page(
    vararg items: Notification,
    nextCursor: String? = null,
    lastReadNotificationId: String? = null,
    serverUnreadCount: Int? = null,
) = NotificationPage(
    items = items.toList(),
    nextCursor = nextCursor,
    retentionDays = 180,
    lastReadNotificationId = lastReadNotificationId,
    serverUnreadCount = serverUnreadCount,
)

internal fun settings(
    pushEnabled: Boolean = true,
    account: Boolean = true,
    challenge: Boolean = true,
    marketing: Boolean = true,
    muted: List<String> = emptyList(),
) = NotificationSettings(
    pushEnabled = pushEnabled,
    groups = NotificationGroupSettings(account = account, challenge = challenge, marketing = marketing),
    mutedChallengeIds = muted,
)
