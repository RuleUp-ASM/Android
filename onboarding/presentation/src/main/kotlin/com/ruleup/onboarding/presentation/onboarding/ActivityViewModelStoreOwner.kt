package com.ruleup.onboarding.presentation.onboarding

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelStoreOwner

@Composable
fun rememberActivityViewModelStoreOwner(): ViewModelStoreOwner = LocalActivity.current as ComponentActivity
