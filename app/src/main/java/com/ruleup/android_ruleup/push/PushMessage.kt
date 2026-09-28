package com.ruleup.android_ruleup.push

/** 서버가 보낸 푸시 한 건. */
data class PushMessage(
    // 알림 센터 행 ID.
    val notificationId: String,
    val title: String,
    val body: String,
    val deeplink: String?,
) {
    companion object {
        private const val KEY_NOTIFICATION_ID = "notification_id"
        private const val KEY_DEEPLINK = "deeplink"

        /** 표시할 수 있는 푸시인지 판정해 만든다. */
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
