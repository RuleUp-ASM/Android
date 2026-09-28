package com.ruleup.ui.helper

import androidx.compose.runtime.staticCompositionLocalOf
import com.ruleup.logging.domain.BizLogger

/** 생성자 주입을 못 받는 Composable 용 [BizLogger] 경로. */
val LocalBizLogger =
    staticCompositionLocalOf<BizLogger> { error("LocalBizLogger 가 제공되지 않았습니다.") }
