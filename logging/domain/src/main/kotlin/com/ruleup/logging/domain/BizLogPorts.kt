package com.ruleup.logging.domain

/**
 * 시각을 읽는 포트.
 *
 * 단조 시계가 필요 없다 — 구간 길이를 재는 것이 아니라 "언제 일어난 일인가" 만 남기므로
 * 사람이 읽는 벽시계 시각이어야 한다.
 */
fun interface BizLogClock {
    fun nowMillis(): Long
}

/** 기본 시계. 순수 JVM 이라 이 모듈 안에 둔다 — 안드로이드 구현을 따로 꽂을 것이 없다. */
class SystemBizLogClock : BizLogClock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}

/**
 * 지금 보고 있는 화면을 알려주는 포트. 네비게이션을 쥔 `:app` 이 구현한다.
 *
 * 이벤트마다 불리므로 필드 읽기 수준으로 저렴해야 하고, 임의 스레드에서 불리며 던지지 않는다.
 * 화면을 특정할 수 없으면 null — 그때는 화면 없이 기록한다.
 */
fun interface BizScreenSource {
    fun currentScreen(): String?
}

/**
 * 사용자 식별자를 알려주는 포트. 세션을 쥔 쪽(`:app`)이 구현한다.
 *
 * 이벤트에 실려 나가는 값이라 **불투명 식별자**여야 한다 — 이메일·이름·전화번호를 넣지 않는다.
 * 로그인 전이면 null 이고, 그 상태로도 기록은 나간다.
 */
fun interface BizUserSource {
    fun currentUserId(): String?
}

/**
 * 이벤트를 내보내는 곳.
 *
 * 이 모듈은 "무엇을 언제 남기는가" 까지만 안다. 어디로 보내는지는 구현이 정하고,
 * 지금 꽂혀 있는 것은 `:logging:data` 의 Firebase·Amplitude 전송기다.
 *
 * 기록된 순서대로, 한 번에 하나씩, IO 디스패처에서 불린다.
 */
fun interface BizLogShooter {
    /**
     * 실패하면 그대로 예외를 던진다. 기록기가 받아 삼키고 다음 건으로 넘어간다 —
     * 쌓아 두는 곳이 없으므로 실패한 건은 그대로 사라진다.
     */
    suspend fun shoot(log: BizLog)
}
