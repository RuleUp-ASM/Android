package com.ruleup.ui.helper

import androidx.compose.runtime.compositionLocalOf
import com.ruleup.domain.helper.NavigationHelper

/** 내비게이션 헬퍼 주입. */
val LocalNavigationHelper = compositionLocalOf<NavigationHelper> { error("No user found!") }
