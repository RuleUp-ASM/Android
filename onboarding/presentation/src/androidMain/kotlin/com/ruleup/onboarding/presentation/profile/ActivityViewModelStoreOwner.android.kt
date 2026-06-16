package com.ruleup.onboarding.presentation.profile

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelStoreOwner

@Composable
actual fun rememberActivityViewModelStoreOwner(): ViewModelStoreOwner = LocalActivity.current as ComponentActivity
