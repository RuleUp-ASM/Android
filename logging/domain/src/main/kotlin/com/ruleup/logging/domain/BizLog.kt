package com.ruleup.logging.domain

/**
 * 기록된 이벤트 한 건. [BizLogShooter] 로 나가는 것이 이 모양이다.
 *
 * 쌓아 두지 않고 [BizLogger.record] 때마다 하나씩 나간다.
 *
 * 화면과 사용자를 기록기가 아니라 건마다 붙이는 이유는, 전송이 IO 로 넘어가 나중에 도는 사이
 * 화면이 바뀌거나 로그아웃될 수 있기 때문이다. 먼저 기록된 것은 일어난 시점의 값을 달고 나간다.
 *
 * @param screen 이 이벤트가 일어난 화면 경로. 특정할 수 없으면 null 이다.
 * @param userId 서버 userId 같은 **불투명 식별자**. 로그인 전이면 null 이다.
 * @param recordedAt 기록된 벽시계 시각(ms). 쏘는 시각과 일어난 시각이 다르므로 따로 남긴다.
 */
data class BizLog(
    val event: BizEvent,
    val screen: String?,
    val userId: String?,
    val recordedAt: Long,
)
