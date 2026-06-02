package com.ruleup.core.navigation

import android.net.Uri
import androidx.navigation3.runtime.NavKey

object DeepLinkResolver {
    fun resolve(uri: Uri?): List<NavKey> {
        if (uri == null) return listOf(OnboardingIntroKey)
        return when (uri.pathSegments.firstOrNull()) {
            "signup" -> {
                uri
                    .getQueryParameter("token")
                    ?.let { listOf(LoginKey, SignupKey(it)) }
                    ?: listOf(LoginKey)
            }

            "login" -> {
                listOf(LoginKey)
            }

            else -> {
                listOf(LoginKey)
            }
        }
    }
}
