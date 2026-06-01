package com.ruleup.core.navigation

import android.net.Uri
import androidx.navigation3.runtime.NavKey

/**
 * 앱링크/딥링크 Uri 를 백스택([List]<[NavKey]>)으로 변환한다.
 *
 * Nav3 는 자동 딥링크 매칭이 없으므로 진입 시 백스택을 직접 구성한다.
 * 리스트를 반환하는 이유: 딥링크로 하위 화면을 열어도 뒤로가기가 자연스럽도록
 * 상위 화면까지 포함한 스택을 세팅하기 위함.
 */
object DeepLinkResolver {
    fun resolve(uri: Uri?): List<NavKey> {
        if (uri == null) return listOf(OnboardingIntroKey) // 평소 진입 (추후 Splash 에서 세션/시청여부로 분기)
        return when (uri.pathSegments.firstOrNull()) {
            "signup" ->
                uri
                    .getQueryParameter("token")
                    ?.let { listOf(LoginKey, SignupKey(it)) }
                    ?: listOf(LoginKey)

            "login" -> listOf(LoginKey)

            else -> listOf(LoginKey)
        }
    }
}
