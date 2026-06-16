package com.ruleup.ui.helper

import androidx.compose.runtime.compositionLocalOf
import com.ruleup.domain.helper.MessageHelper

val LocalMessageHelper = compositionLocalOf<MessageHelper> { error("No user found!") }
