package com.ruleup.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object OnboardingIntroKey : NavKey

@Serializable
data object LoginKey : NavKey

@Serializable
data class SignupKey(
    val signupToken: String,
) : NavKey

@Serializable
data object HomeKey : NavKey
