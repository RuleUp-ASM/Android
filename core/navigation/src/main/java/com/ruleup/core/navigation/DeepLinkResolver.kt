package com.ruleup.core.navigation

import android.net.Uri
import androidx.navigation3.runtime.NavKey

object DeepLinkResolver {
    fun resolve(
        uri: Uri?,
        isLoggedIn: Boolean,
    ): List<NavKey> {
        // 딥링크 없는 일반 cold start: 이미 로그인된 사용자면 온보딩/로그인을 건너뛰고 바로 홈으로.
        if (uri == null) return if (isLoggedIn) listOf(HomeKey) else listOf(OnboardingIntroKey)
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
