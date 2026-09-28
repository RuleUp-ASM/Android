package com.ruleup.logging.domain

/** 남길 수 있는 비즈니스 이벤트 하나. */
data class BizEvent(
    val name: String,
    val attrs: BizAttributes = BizAttributes.EMPTY,
)

/** 어느 feature 에도 속하지 않는 공통 이벤트. */
object CommonBizEvents {
    /** 화면에 들어왔다. */
    fun screenView(
        screen: String,
        from: String? = null,
    ) = BizEvent(
        "screen_view",
        bizAttributes {
            put("screen_name", screen)
            from?.let { put("from_screen", it) }
        },
    )
}
