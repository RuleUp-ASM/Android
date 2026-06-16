package com.ruleup.shared.navigation

import androidx.compose.runtime.Composable

@Composable
actual fun rememberShowToast(): (String) -> Unit = { message -> println("[Toast] $message") }
