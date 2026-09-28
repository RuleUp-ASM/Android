package com.ruleup.ui.helper

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.ruleup.logging.domain.BizEvent
import com.ruleup.logging.domain.BizLogger

/** 화면 미리보기 환경. */
@Composable
fun PreviewEnvironment(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalNavigationHelper provides NoOpNavigationHelper,
        LocalBizLogger provides PreviewBizLogger,
        content = content,
    )
}

private object PreviewBizLogger : BizLogger {
    override fun init() = Unit

    override fun record(event: BizEvent) = Unit

    override fun destroy() = Unit
}
