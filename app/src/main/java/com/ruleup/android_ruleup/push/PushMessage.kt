package com.ruleup.android_ruleup.push

/**
 * 서버가 보낸 푸시 한 건 (알림 테크 스펙 7).
 *
 * FCM 은 `notification` 과 `data` 를 **함께** 싣는다 — `data` 만 쓰면 일부 제조사에서 종료 상태에
 * 전달되지 않고, `notification` 만 쓰면 [notificationId] 와 [deeplink] 를 실을 수 없다.
 * `data` 키만 snake_case 다.
 */
data class PushMessage(
    // 알림 센터 행 ID. 트레이 tag 이자 중복 방어의 키다
    val notificationId: String,
    val title: String,
    val body: String,
    val deeplink: String?,
) {
    companion object {
        private const val KEY_NOTIFICATION_ID = "notification_id"
        private const val KEY_DEEPLINK = "deeplink"

        /**
         * 표시할 수 있는 푸시인지 판정해 만든다.
         *
         * **식별자나 제목이 없으면 만들지 않는다** — tag 가 없으면 중복 방어가 깨지고, 제목이 없으면
         * 빈 알림이 트레이에 남는다. 그런 메시지는 조용히 버리는 편이 낫다(알림 센터에는 이미 있다).
         */
        fun from(
            data: Map<String, String>,
            title: String?,
            body: String?,
        ): PushMessage? {
            val id = data[KEY_NOTIFICATION_ID]?.takeIf { it.isNotBlank() } ?: return null
            val resolvedTitle = title?.takeIf { it.isNotBlank() } ?: return null
            return PushMessage(
                notificationId = id,
                title = resolvedTitle,
                body = body.orEmpty(),
                deeplink = data[KEY_DEEPLINK]?.takeIf { it.isNotBlank() },
            )
        }
    }
}
