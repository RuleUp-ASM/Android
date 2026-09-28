package com.ruleup.logging.domain

/** 시각을 읽는 포트. */
fun interface BizLogClock {
    fun nowMillis(): Long
}

/** 기본 시계. */
class SystemBizLogClock : BizLogClock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}

/** 지금 보고 있는 화면을 알려주는 포트. */
fun interface BizScreenSource {
    fun currentScreen(): String?
}

/** 사용자 식별자를 알려주는 포트. */
fun interface BizUserSource {
    fun currentUserId(): String?
}

/** 이벤트를 내보내는 곳. */
fun interface BizLogShooter {
    /** 실패하면 그대로 예외를 던진다. */
    suspend fun shoot(log: BizLog)
}
