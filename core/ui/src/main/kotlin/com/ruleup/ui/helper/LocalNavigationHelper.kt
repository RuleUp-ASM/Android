package com.ruleup.ui.helper

import androidx.compose.runtime.compositionLocalOf
import com.ruleup.domain.helper.NavigationHelper

/** 구현체는 app 이 바인딩한다([com.ruleup.domain.helper.NavigationHelper] 는 core:domain 계약). */
val LocalNavigationHelper = compositionLocalOf<NavigationHelper> { error("No user found!") }
