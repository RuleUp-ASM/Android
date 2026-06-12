package com.ruleup.android_ruleup.deeplink

import android.net.Uri
import android.util.Log
import androidx.navigation3.runtime.NavKey
import com.ruleup.shared.navigation.GenericNavKey
import com.ruleup.shared.navigation.appRouteByPath
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.onboarding.domain.IntroPromisePage
import com.ruleup.onboarding.domain.SplashPage

private const val TAG = "[DeepLink]"

/**
 * App Link 의 [Uri] 를 [NavRoute] 로 변환한다.
 * - path: pathSegments 를 슬래시로 합쳐 등록된 PATH 와 동일한 형식으로 만든다 (앞 슬래시 없음, 예: "profile/icon").
 * - args: 모든 query parameter 를 그대로 String 맵으로 옮긴다 (복합 타입은 호출부의 Args.from 이 디코딩).
 */
fun Uri.toNavRoute(): NavRoute {
    val segments = pathSegments?.takeIf { it.isNotEmpty() } ?: return NavRoute(IntroPromisePage.PATH)
    val path = segments.joinToString("/")
    val args =
        queryParameterNames
            .filter { it.isNotEmpty() }
            .associateWith { (getQueryParameter(it) ?: "") }
    return NavRoute(path, args)
}

/**
 * App Link 진입 시 시작 백스택을 구성한다.
 * - URI 가 없거나 미등록 path 면 Intro 단일 스택으로 fallback.
 * - 등록된 path 면 해당 [com.ruleup.android_ruleup.navigation.AppRoute] 의
 *   syntheticStack 을 그대로 사용한다.
 */
fun resolveStartStack(uri: Uri?): List<NavKey> {
    // 일반 실행(딥링크 없음)은 스플래시에서 시작해 자동 로그인 여부로 홈/인트로를 분기한다.
    if (uri == null) return listOf(GenericNavKey(SplashPage.PATH))
    val route = uri.toNavRoute()
    val appRoute = appRouteByPath[route.path]
    if (appRoute == null) {
        Log.w(TAG, "No matching path for uri=$uri (path=${route.path})")
        return listOf(GenericNavKey(IntroPromisePage.PATH))
    }
    return appRoute.syntheticStack(route.args)
}

/**
 * 앱 실행 중 들어온 새 deep-link 를 처리할 [NavRoute] 로 변환.
 * 미등록 path 면 null 반환 (호출부가 무시 결정).
 */
fun resolveNewIntentRoute(uri: Uri): NavRoute? {
    val route = uri.toNavRoute()
    if (appRouteByPath[route.path] == null) {
        Log.w(TAG, "onNewIntent: unhandled uri=$uri (path=${route.path})")
        return null
    }
    return route
}
