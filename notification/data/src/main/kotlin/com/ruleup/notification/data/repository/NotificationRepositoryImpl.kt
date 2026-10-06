package com.ruleup.notification.data.repository

import com.ruleup.network.dto.ApiException
import com.ruleup.network.dto.getOrThrow
import com.ruleup.notification.data.api.NotificationApi
import com.ruleup.notification.data.api.throwOnFailure
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
                .throwOnFailure()
        }

        /** 미읽음 집계. */
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
                    api.mute(challengeId).throwOnFailure()
                } else {
                    api.unmute(challengeId).throwOnFailure()
                }
            } catch (e: ApiException) {
                // 참여하지 않은 방이면 화면이 목록을 갱신해야 한다
                throw e.toMuteFailure()
            }
        }
    }
