package com.ruleup.notification.data.repository

import com.ruleup.network.dto.ApiException
import com.ruleup.network.dto.getOrThrow
import com.ruleup.network.dto.throwOnError
import com.ruleup.notification.data.api.NotificationApi
import com.ruleup.notification.data.dto.MarkReadRequest
import com.ruleup.notification.data.dto.toDomain
import com.ruleup.notification.data.dto.toMuteFailure
import com.ruleup.notification.data.dto.toRequest
import com.ruleup.notification.domain.entity.NotificationPage
import com.ruleup.notification.domain.entity.NotificationSettings
import com.ruleup.notification.domain.entity.NotificationSettingsResult
import com.ruleup.notification.domain.entity.NotificationSettingsUpdate
import com.ruleup.notification.domain.entity.NotificationTab
import com.ruleup.notification.domain.entity.UnreadSummary
import com.ruleup.notification.domain.repository.NotificationRepository
import javax.inject.Inject

class NotificationRepositoryImpl
    @Inject
    constructor(
        private val api: NotificationApi,
    ) : NotificationRepository {
        override suspend fun getNotifications(
            tab: NotificationTab,
            cursor: String?,
        ): NotificationPage =
            api
                .getNotifications(tab = tab.value, cursor = cursor)
                .getOrThrow()
                .toDomain()

        override suspend fun markRead(
            tab: NotificationTab,
            lastNotificationId: String,
        ) {
            api
                .markRead(MarkReadRequest(tab = tab.value, lastNotificationId = lastNotificationId))
                // 204 면 본문이 없다 — 실패했을 때만 봉투가 온다.
                ?.throwOnError()
        }

        /**
         * 미읽음 집계.
         *
         * 서버가 세어 주지 않으므로 목록을 읽어 센다. 기준선을 만나면 거기서 멈추고, 못 만나면
         * 상한([NotificationPage.MAX_UNREAD_PAGES])까지만 더 읽는다 — 카운터가 `99+` 라 그 이상
         * 세도 화면에 쓸 데가 없다.
         *
         * 기준선을 아예 안 주는 서버(구 계약)에서는 첫 페이지의 `unreadCount` 를 그대로 쓴다.
         * 그때는 챌린지별로 나눌 근거가 없어 총계만 남는다.
         *
         * **알림 탭만 센다.** 운영자 공지는 읽음 지점이 따로 보관되고 레드닷·챌린지 카운터의
         * 대상도 아니다(테크 스펙 5-2) — 섞어 세면 공지 하나로 챌린지 카드에 숫자가 뜬다.
         */
        override suspend fun getUnreadSummary(): UnreadSummary {
            var cursor: String? = null
            var total = 0
            val byChallenge = mutableMapOf<String, Int>()

            repeat(NotificationPage.MAX_UNREAD_PAGES) { page ->
                val response =
                    api
                        .getNotifications(tab = NotificationTab.NOTIFICATION.value, cursor = cursor)
                        .getOrThrow()
                        .toDomain()
                val serverCount = response.serverUnreadCount
                if (page == 0 && response.lastReadNotificationId == null && serverCount != null) {
                    return UnreadSummary(total = serverCount, byChallenge = emptyMap())
                }
                response.unread.forEach { notification ->
                    total++
                    notification.challengeId?.let { byChallenge[it] = (byChallenge[it] ?: 0) + 1 }
                }
                cursor = response.nextCursor
                // 기준선을 만났거나 더 읽을 페이지가 없으면 멈춘다.
                if (response.boundaryFound || cursor == null) return UnreadSummary(total, byChallenge)
            }
            return UnreadSummary(total, byChallenge)
        }

        override suspend fun getSettings(): NotificationSettings =
            api
                .getSettings()
                .getOrThrow()
                .toDomain()

        override suspend fun updateSettings(update: NotificationSettingsUpdate): NotificationSettingsResult =
            api
                .updateSettings(update.toRequest())
                .getOrThrow()
                .toDomain()

        override suspend fun setMuted(
            challengeId: String,
            muted: Boolean,
        ) {
            try {
                if (muted) {
                    api.mute(challengeId)?.throwOnError()
                } else {
                    api.unmute(challengeId)?.throwOnError()
                }
            } catch (e: ApiException) {
                // 참여하지 않은 방이면 화면이 목록을 갱신해야 한다 — 일반 오류로 접지 않는다.
                throw e.toMuteFailure()
            }
        }
    }
