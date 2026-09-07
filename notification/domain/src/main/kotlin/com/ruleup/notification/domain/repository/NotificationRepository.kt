package com.ruleup.notification.domain.repository

import com.ruleup.notification.domain.entity.NotificationPage
import com.ruleup.notification.domain.entity.NotificationSettings
import com.ruleup.notification.domain.entity.NotificationSettingsResult
import com.ruleup.notification.domain.entity.NotificationSettingsUpdate
import com.ruleup.notification.domain.entity.NotificationTab
import com.ruleup.notification.domain.entity.UnreadSummary

/**
 * 알림 센터와 푸시 설정.
 *
 * **잠금 계정도 열려야 한다** — 제재 고지가 알림 센터에 쌓이므로, 여기가 막히면 사용자가 왜
 * 잠겼는지 알 방법이 없다(테크 스펙 12·오픈 이슈 #10).
 */
interface NotificationRepository {
    /**
     * 알림 센터 한 페이지(명세: GET /notifications).
     *
     * **페이지 크기는 서버 고정 50**이라 인자로 받지 않는다. [cursor] 가 없으면 첫 페이지다.
     * 잘못된 커서는 서버가 400 `CURSOR_INVALID` 로 막으므로 클라가 보정하지 않는다.
     *
     * [tab] 은 알림과 운영자 공지를 가른다. **읽음 지점이 탭별로 따로 보관**되므로 응답의
     * 기준선도 요청한 탭의 것이고, 두 탭의 미읽음을 섞어 세면 안 된다.
     */
    suspend fun getNotifications(
        tab: NotificationTab = NotificationTab.NOTIFICATION,
        cursor: String? = null,
    ): NotificationPage

    /**
     * 읽음 지점 갱신(명세: PUT /notifications/read).
     *
     * [lastNotificationId] 는 **응답에 실제로 담겼던 최신 항목의 id** 여야 한다 — 현재 시각으로
     * 갱신하면 조회와 갱신 사이에 적재된 알림이 화면에 뜬 적 없이 읽음 처리된다.
     * 첫 페이지 조회 직후에만 부른다. 커서 페이징에서 부르면 읽음 지점이 과거로 밀린다.
     */
    suspend fun markRead(
        tab: NotificationTab,
        lastNotificationId: String,
    )

    /**
     * 레드닷·챌린지 카운터용 미읽음 집계.
     *
     * 서버가 세어 주지 않으므로 목록을 읽어 클라가 센다. 상한이 `99+` 라
     * [NotificationPage.MAX_UNREAD_PAGES] 장까지만 읽는다 — 그 이상은 화면에 쓸 데가 없다.
     */
    suspend fun getUnreadSummary(): UnreadSummary

    suspend fun getSettings(): NotificationSettings

    /** 보낸 필드만 바꾼다. 마케팅을 바꾸면 서버가 수신 동의까지 같은 트랜잭션으로 처리한다. */
    suspend fun updateSettings(update: NotificationSettingsUpdate): NotificationSettingsResult

    /**
     * 챌린지별 음소거 등록·해제(명세: PUT/DELETE .../mutes/{challengeId}). 멱등이다.
     *
     * 참여 중인 챌린지만 등록되며, 아니면
     * [com.ruleup.notification.domain.entity.ChallengeNotJoinedException] 이 던져진다.
     * 루틴 종료·탈퇴 시에는 서버가 알아서 지우므로 클라가 해제하지 않아도 된다.
     */
    suspend fun setMuted(
        challengeId: String,
        muted: Boolean,
    )
}
